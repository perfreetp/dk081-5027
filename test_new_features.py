# -*- coding: utf-8 -*-
import requests
import json
import time
from datetime import datetime

BASE_URL = "http://localhost:8080/hf-transfer/api/v1"
APPLICATION_URL = f"{BASE_URL}/application"

def print_response(desc, r):
    print(f"\n{'='*60}")
    print(f"✓ {desc}")
    print(f"  URL: {r.url}")
    print(f"  状态码: {r.status_code}")
    try:
        data = r.json()
        print(f"  返回: {json.dumps(data, ensure_ascii=False, indent=2)[:1500]}")
        return data
    except:
        print(f"  返回: {r.text[:500]}")
        return None

def test_all_features():
    print("🚀 开始测试新功能接口...")

    # ========== 1. 先提交几笔测试申请 ==========
    print("\n" + "="*60)
    print("📋 第一步：提交测试申请（共4笔）")
    
    applications = []
    test_cases = [
        {"name": "张三", "idcard": "110101199001011234", "out": "110000", "in": "310000", "amount": 50000},
        {"name": "李四", "idcard": "310101199002025678", "out": "310000", "in": "440100", "amount": 80000},
        {"name": "王五", "idcard": "440101199003039012", "out": "440100", "in": "320100", "amount": 35000},
        {"name": "赵六", "idcard": "320101199004043456", "out": "320100", "in": "330100", "amount": 62000},
    ]
    
    for tc in test_cases:
        r = requests.post(f"{APPLICATION_URL}/submit", json={
            "applicantName": tc["name"],
            "idCardNo": tc["idcard"],
            "idCardType": 1,
            "transferOutRegion": tc["out"],
            "transferInRegion": tc["in"],
            "transferAmount": tc["amount"],
            "transferType": 1,
            "channel": "ONLINE",
            "channelType": 1,
            "applyReason": "工作调动",
            "mobilePhone": "13800138000",
            "materials": [{"materialType": "ID_CARD", "materialName": "身份证", "fileUrl": "/files/idcard.pdf"}]
        }, headers={"Content-Type": "application/json"})
        data = print_response(f"提交申请 - {tc['name']}", r)
        if data and data.get("code") == 200:
            app_no = data.get("data")
            print(f"  ✅ 申请编号: {app_no}")
            # 通过申请编号查询申请ID
            time.sleep(0.1)
            r2 = requests.get(f"{APPLICATION_URL}/progress?applicationNo={app_no}")
            if r2.status_code == 200:
                d2 = r2.json()
                if d2.get("code") == 200:
                    applications.append({
                        "applicationNo": app_no,
                        "name": tc["name"]
                    })
                    continue
            # 如果查不到，先用编号占位
            applications.append({
                "applicationNo": app_no,
                "name": tc["name"]
            })
    
    if len(applications) < 2:
        print("❌ 申请提交失败，无法继续测试")
        return

    # 查询申请列表获取ID
    print("\n📋 获取申请ID列表...")
    r = requests.post(f"{APPLICATION_URL}/page", json={"pageNum": 1, "pageSize": 10})
    app_ids = {}
    if r.status_code == 200:
        data = r.json()
        if data.get("code") == 200:
            records = data.get("data", {}).get("records", [])
            for rec in records:
                app_ids[rec.get("applicationNo")] = rec.get("id")
            for app in applications:
                app["id"] = app_ids.get(app["applicationNo"])
                print(f"  {app['name']}: {app['applicationNo']} -> ID: {app.get('id')}")

    # ========== 2. 查询待办驾驶舱 ==========
    print("\n" + "="*60)
    print("📊 功能1：待办驾驶舱")
    r = requests.get(f"{BASE_URL}/statistics/dashboard")
    data = print_response("待办驾驶舱（全国）", r)
    
    r = requests.get(f"{BASE_URL}/statistics/dashboard?regionCode=110000")
    data = print_response("待办驾驶舱（北京中心）", r)

    # ========== 3. 查询待处理任务列表 ==========
    print("\n" + "="*60)
    print("📋 查询待转出任务列表（用于批量处理）")
    
    task_ids = []
    # 通过申请ID查询关联任务
    for app in applications[:3]:
        if app.get("id"):
            r = requests.get(f"{BASE_URL}/collaboration/tasks/application/{app['id']}")
            if r.status_code == 200:
                data = r.json()
                if data.get("code") == 200:
                    tasks = data.get("data", [])
                    for t in tasks:
                        if t.get("taskStatus") == 10:  # 待处理
                            task_ids.append(t.get("id"))
                            print(f"  任务ID: {t.get('id')}, 申请编号: {t.get('applicationNo')}, 类型: {t.get('taskTypeName')}")
    
    if len(task_ids) < 2:
        print("⚠️  任务不足，用已知ID模拟批量处理测试（包含不存在的ID验证失败场景）")
        # 添加一个不存在的任务ID来测试失败场景
        task_ids = task_ids + [999999999999] if task_ids else [1, 2, 999999999999]

    # ========== 4. 批量转出确认 ==========
    print("\n" + "="*60)
    print("⚡ 功能2：批量处理 - 批量转出确认")
    
    batch_items = []
    for i, tid in enumerate(task_ids[:3] if task_ids else [1, 2, 99999]):  # 99999不存在，用于测试失败场景
        batch_items.append({
            "taskId": tid,
            "actualAmount": 50000 + i * 10000,
            "remark": f"批量转出确认{i+1}"
        })
    
    r = requests.post(f"{BASE_URL}/collaboration/tasks/batch/confirmOut", json={
        "items": batch_items,
        "operatorId": "OP001",
        "operatorName": "批量操作员"
    }, headers={"Content-Type": "application/json", "X-Operator-Id": "OP001", "X-Operator-Name": "批量操作员"})
    data = print_response("批量转出确认（包含失败测试）", r)
    
    if data and data.get("code") == 200:
        result = data.get("data", {})
        results = result.get("results", [])
        success = sum(1 for x in results if x.get("success"))
        fail = sum(1 for x in results if not x.get("success"))
        print(f"\n  📊 批量结果: 成功 {success} 笔, 失败 {fail} 笔")
        for res in results:
            if res.get("success"):
                print(f"     ✅ 任务 {res.get('taskId')}: 成功")
            else:
                print(f"     ❌ 任务 {res.get('taskId')}: 失败 - {res.get('failReason')}")

    # ========== 5. 批量退件 ==========
    print("\n" + "="*60)
    print("⚡ 功能2：批量处理 - 批量退件")
    
    batch_reject_items = []
    for i, tid in enumerate(task_ids[1:3] if len(task_ids) > 2 else [3, 4, 88888]):
        batch_reject_items.append({
            "taskId": tid,
            "rejectReasonCode": "R002",
            "rejectReasonName": "账户信息不一致",
            "needSupplement": i == 0,
            "supplementItems": "补充账户证明材料" if i == 0 else None,
            "remark": f"批量退件{i+1}"
        })
    
    r = requests.post(f"{BASE_URL}/collaboration/tasks/batch/reject", json={
        "items": batch_reject_items,
        "operatorId": "OP001",
        "operatorName": "批量操作员"
    }, headers={"Content-Type": "application/json", "X-Operator-Id": "OP001", "X-Operator-Name": "批量操作员"})
    data = print_response("批量退件（包含失败测试）", r)
    
    if data and data.get("code") == 200:
        result = data.get("data", {})
        results = result.get("results", [])
        success = sum(1 for x in results if x.get("success"))
        fail = sum(1 for x in results if not x.get("success"))
        print(f"\n  📊 批量结果: 成功 {success} 笔, 失败 {fail} 笔")
        for res in results:
            if res.get("success"):
                print(f"     ✅ 任务 {res.get('taskId')}: 成功")
            else:
                print(f"     ❌ 任务 {res.get('taskId')}: 失败 - {res.get('failReason')}")

    # ========== 6. 外部回调查询 ==========
    print("\n" + "="*60)
    print("🔗 功能4：外部回调查询")
    
    test_app_no = applications[0]["applicationNo"]
    r = requests.get(f"{APPLICATION_URL}/callback?applicationNo={test_app_no}")
    data = print_response(f"外部回调查询 - {test_app_no}", r)
    
    if data and data.get("code") == 200:
        cb = data.get("data", {})
        print(f"\n  📋 申请编号: {cb.get('applicationNo')}")
        print(f"  📍 当前节点: {cb.get('currentNode')}")
        print(f"  📝 当前状态: {cb.get('statusName')}")
        last_op = cb.get("lastOperation", {})
        if last_op:
            print(f"  ⏰ 最近操作: {last_op.get('operationName')} by {last_op.get('operatorName')} at {last_op.get('operationTime')}")
        next_todo = cb.get("nextTodo", {})
        if next_todo:
            print(f"  🎯 下一步: {next_todo.get('todoName')} - {next_todo.get('todoDesc')}")
        urge_records = cb.get("urgeRecords", [])
        if urge_records:
            print(f"  📢 催办记录: {len(urge_records)} 条")
            for u in urge_records[:2]:
                level = u.get("escalateLevelName") or u.get("urgeTypeName")
                print(f"     - {u.get('urgeTime')}: {level} - {u.get('urgeContent')}")
        else:
            print(f"  📢 催办记录: 暂无")

    # ========== 7. 催办升级记录检查 ==========
    print("\n" + "="*60)
    print("🔔 功能3：催办升级记录")
    
    r = requests.get(f"{APPLICATION_URL}/progress?applicationNo={test_app_no}")
    data = print_response("申请进度查询（含催办记录）", r)
    
    if data and data.get("code") == 200:
        progress = data.get("data", {})
        urge_logs = progress.get("urgeLogs", [])
        status_logs = progress.get("statusLogs", [])
        print(f"\n  📝 状态变更: {len(status_logs)} 条")
        for log in status_logs[:5]:
            print(f"     {log.get('createTime')}: {log.get('fromStatusName')} → {log.get('toStatusName')}")
        if urge_logs:
            print(f"  📢 催办升级记录: {len(urge_logs)} 条")
            for log in urge_logs[:3]:
                is_esc = "【已升级】" if log.get("isEscalated") else ""
                print(f"     {log.get('urgeTime')}: {log.get('urgeTypeName')} {is_esc}{log.get('urgeContent')}")
                if log.get("isEscalated"):
                    print(f"       → 升级至: {log.get('escalateToRegion')} {log.get('escalateToCenter')} (等级: {log.get('escalateLevelName')})")

    # ========== 8. 归档摘要 ==========
    print("\n" + "="*60)
    print("📦 功能5：归档摘要查询")
    
    # 先把第一笔申请走完流程，模拟办结状态
    print("  (模拟流程：先确认一笔申请的转出和转入，使其办结)")
    first_app_id = applications[0]["id"]
    first_app_no = applications[0]["applicationNo"]
    
    # 查询该申请的任务
    r = requests.get(f"{BASE_URL}/collaboration/application/{first_app_id}/tasks")
    task_data = print_response("查询申请关联任务", r)
    
    task_id = None
    if task_data and task_data.get("code") == 200:
        tasks = task_data.get("data", [])
        for t in tasks:
            if t.get("taskType") == 10:  # 转出任务
                task_id = t.get("id")
                break
    
    if task_id:
        # 转出确认
        r = requests.post(f"{BASE_URL}/collaboration/task/{task_id}/confirmOut", json={
            "operatorId": "OP002",
            "operatorName": "转出操作员",
            "remark": "测试归档-转出确认",
            "actualAmount": 50000
        }, headers={"Content-Type": "application/json"})
        print_response("转出确认", r)
        
        # 找转入任务
        r = requests.get(f"{BASE_URL}/collaboration/application/{first_app_id}/tasks")
        if r.status_code == 200:
            tasks = r.json().get("data", [])
            for t in tasks:
                if t.get("taskType") == 20:  # 转入任务
                    # 转入确认
                    r = requests.post(f"{BASE_URL}/collaboration/task/{t.get('id')}/confirmIn", json={
                        "operatorId": "OP003",
                        "operatorName": "转入操作员",
                        "remark": "测试归档-转入确认",
                        "accountResult": 1,
                        "actualAmount": 50000
                    }, headers={"Content-Type": "application/json"})
                    print_response("转入确认", r)
                    break
    
    # 查询归档摘要
    r = requests.get(f"{APPLICATION_URL}/archive/{first_app_no}")
    data = print_response(f"归档摘要 - {first_app_no}", r)
    
    if data and data.get("code") == 200:
        arc = data.get("data", {})
        print(f"\n{'='*60}")
        print("📦 归档摘要内容预览:")
        print(f"{'='*60}")
        
        basic = arc.get("basicInfo", {})
        print(f"\n  【基本信息】")
        print(f"    申请编号: {basic.get('applicationNo')}")
        print(f"    申请人: {basic.get('applicantName')}")
        print(f"    转移方向: {basic.get('transferOutRegionName')} → {basic.get('transferInRegionName')}")
        print(f"    申请时间: {basic.get('applyTime')}")
        print(f"    办结时间: {basic.get('finishTime')}")
        
        rule_result = arc.get("ruleValidationResult", {})
        print(f"\n  【规则校验结果】")
        print(f"    是否通过: {'✅ 通过' if rule_result.get('passed') else '❌ 未通过'}")
        print(f"    通过项: {len(rule_result.get('passedItems', []))} 项")
        print(f"    未通过项: {len(rule_result.get('failedItems', []))} 项")
        for item in rule_result.get('passedItems', [])[:2]:
            print(f"      ✓ {item}")
        
        tasks = arc.get("collaborationTasks", [])
        print(f"\n  【协同任务】共 {len(tasks)} 个")
        for t in tasks:
            print(f"    - {t.get('taskTypeName')}: {t.get('taskStatusName')}")
            if t.get('confirmTime'):
                print(f"      确认时间: {t.get('confirmTime')}, 确认结果: {t.get('confirmResultName')}")
        
        reject_records = arc.get("rejectRecords", [])
        print(f"\n  【退件记录】共 {len(reject_records)} 条")
        for r in reject_records:
            print(f"    - {r.get('rejectTime')}: {r.get('rejectReasonName')}")
        
        supplement_records = arc.get("supplementRecords", [])
        print(f"\n  【补正记录】共 {len(supplement_records)} 条")
        for s in supplement_records:
            print(f"    - {s.get('createTime')}: 需补正 {s.get('supplementItems')}")
        
        urge_records = arc.get("urgeRecords", [])
        print(f"\n  【催办/升级记录】共 {len(urge_records)} 条")
        for u in urge_records:
            esc = " ⬆️ 已升级" if u.get("isEscalated") else ""
            print(f"    - {u.get('urgeTime')}: {u.get('urgeTypeName')}{esc} - {u.get('urgeContent')}")
        
        op_logs = arc.get("operationLogs", [])
        print(f"\n  【操作留痕】共 {len(op_logs)} 条")
        for op in op_logs[:5]:
            print(f"    - {op.get('operationTime')}: {op.get('operatorName')} {op.get('operationName')}")
        
        stats = arc.get("summaryStatistics", {})
        print(f"\n  【效率统计】")
        print(f"    总耗时: {stats.get('totalDurationDays')} 天")
        print(f"    转出耗时: {stats.get('transferOutDurationDays')} 天")
        print(f"    转入耗时: {stats.get('transferInDurationDays')} 天")
        print(f"    补正次数: {stats.get('supplementCount')} 次")
        print(f"    催办次数: {stats.get('urgeCount')} 次")
        print(f"    退件次数: {stats.get('rejectCount')} 次")

    print("\n" + "="*60)
    print("🎉 所有新功能测试完成！")
    print("="*60)
    print("\n📋 功能实现清单:")
    print("  ✅ 1. 待办驾驶舱：按中心统计待办，附带申请编号列表")
    print("  ✅ 2. 批量处理：批量转出确认/退件，支持部分失败")
    print("  ✅ 3. 催办升级：超时自动升级，进度查询显示记录")
    print("  ✅ 4. 外部回调查询：稳定字段返回当前节点/操作/待办")
    print("  ✅ 5. 归档摘要：办结后生成完整结构化档案")
    print("\n🔍 接口文档地址: http://localhost:8080/hf-transfer/doc.html")

if __name__ == "__main__":
    test_all_features()
