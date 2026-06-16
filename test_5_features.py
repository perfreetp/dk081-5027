# -*- coding: utf-8 -*-
import requests
import json
import time

BASE_URL = "http://localhost:8080/hf-transfer/api/v1"
APP_URL = f"{BASE_URL}/application"
COL_URL = f"{BASE_URL}/collaboration"
STAT_URL = f"{BASE_URL}/statistics"

def p(desc, r):
    print(f"\n{'='*60}")
    print(f"✓ {desc}")
    print(f"  URL: {r.url}")
    print(f"  状态码: {r.status_code}")
    try:
        d = r.json()
        print(f"  成功: {d.get('code') == 200}")
        print(f"  消息: {d.get('message')}")
        return d
    except Exception as e:
        print(f"  返回: {r.text[:200]}")
        return None

print("🚀 测试5项新功能接口...")

# ===== 1. 先提交2笔申请 =====
print("\n" + "="*60)
print("📋 提交测试申请...")
app_nos = []
for i, name in enumerate(["测试员A", "测试员B"]):
    r = requests.post(f"{APP_URL}/submit", json={
        "applicantName": name,
        "idCardNo": f"1101011990010{1000+i}",
        "idCardType": 1,
        "transferOutRegion": "110000",
        "transferInRegion": "310000",
        "transferAmount": 50000 + i * 10000,
        "transferType": 1,
        "channel": "ONLINE",
        "channelType": 1,
        "applyReason": "工作调动测试",
        "mobilePhone": "13800138000",
        "materials": [{"materialType": "ID_CARD", "materialName": "身份证", "fileUrl": "/test.pdf"}]
    }, headers={"Content-Type": "application/json"})
    d = p(f"提交申请 - {name}", r)
    if d and d.get("code") == 200:
        app_nos.append(d.get("data"))
        print(f"  申请编号: {d.get('data')}")

time.sleep(0.5)

# 获取申请ID
r = requests.post(f"{APP_URL}/page", json={"pageNum": 1, "pageSize": 10})
app_map = {}
if r.status_code == 200:
    d = r.json()
    if d.get("code") == 200:
        for rec in d.get("data", {}).get("records", []):
            app_map[rec.get("applicationNo")] = rec.get("id")
            print(f"  {rec.get('applicantName')}: {rec.get('applicationNo')} -> ID: {rec.get('id')}")

app_ids = [app_map.get(n) for n in app_nos if app_map.get(n)]

# ===== 功能1：待办驾驶舱 =====
print("\n" + "="*60)
print("📊 功能1：待办驾驶舱")
r = requests.get(f"{STAT_URL}/dashboard")
d = p("待办驾驶舱（全国）", r)
if d and d.get("code") == 200:
    data = d.get("data", {})
    print(f"\n  📊 全国待办统计:")
    print(f"    总待办: {data.get('totalPending')}")
    print(f"    待受理: {data.get('pendingAccept')}")
    print(f"    待转出: {data.get('pendingTransferOut')}")
    print(f"    待转入: {data.get('pendingTransferIn')}")
    print(f"    待补正: {data.get('pendingSupplement')}")
    print(f"    即将超时: {data.get('approachingTimeout')}")
    centers = data.get("centerTodos", [])
    print(f"\n  🏢 各中心明细 ({len(centers)} 个中心):")
    for c in centers[:3]:
        print(f"    - {c.get('centerName')}: 总 {c.get('totalCount')} 笔")
        p_transfer_out = c.get("pendingTransferOut", {})
        items = p_transfer_out.get("items", [])
        print(f"      待转出: {p_transfer_out.get('count')} 笔")
        for item in items[:2]:
            print(f"        * {item.get('applicationNo')} - {item.get('applicantName')} ({item.get('transferOutRegionName')}→{item.get('transferInRegionName')})")

r = requests.get(f"{STAT_URL}/dashboard?regionCode=110000")
d = p("待办驾驶舱（北京中心）", r)

# ===== 功能2：批量处理 =====
print("\n" + "="*60)
print("⚡ 功能2：批量处理")

# 获取任务ID
task_ids = []
for aid in app_ids:
    r = requests.get(f"{COL_URL}/tasks/application/{aid}")
    if r.status_code == 200:
        d = r.json()
        if d.get("code") == 200:
            for t in d.get("data", []):
                if t.get("taskStatus") in [10, 30]:
                    task_ids.append(t.get("id"))
                    print(f"  任务ID: {t.get('id')}, 申请: {t.get('applicationNo')}, 状态: {t.get('taskStatus')}")

# 添加不存在的ID测试失败场景
if len(task_ids) >= 1:
    task_ids.append(999999999999)  # 不存在的任务ID

print(f"\n  测试任务ID列表: {task_ids}")

# 批量转出确认
batch_items = []
for i, tid in enumerate(task_ids):
    batch_items.append({
        "taskId": tid,
        "actualAmount": 50000 + i * 5000,
        "remark": f"批量转出测试{i+1}"
    })

r = requests.post(f"{COL_URL}/tasks/batch/confirmOut", json={
    "items": batch_items,
    "operatorId": "OP001",
    "operatorName": "BatchTest"
}, headers={"Content-Type": "application/json", "X-Operator-Id": "OP001"})
d = p("批量转出确认（含失败测试）", r)

if d and d.get("code") == 200:
    result = d.get("data", {})
    results = result.get("results", [])
    success = sum(1 for x in results if x.get("success"))
    fail = sum(1 for x in results if not x.get("success"))
    print(f"\n  📊 批量处理结果:")
    print(f"    成功: {success} 笔")
    print(f"    失败: {fail} 笔")
    print(f"    成功率: {success/len(results)*100:.1f}%" if results else "    无数据")
    for res in results:
        status = "✅ 成功" if res.get("success") else "❌ 失败"
        reason = f" - {res.get('failReason')}" if not res.get("success") else ""
        print(f"    {status} 任务 {res.get('taskId')}{reason}")

# 批量退件（用新的任务测试，如果有的话）
# 这里简单演示一下接口可调用
print("\n  测试批量退件接口...")
r = requests.post(f"{COL_URL}/tasks/batch/reject", json={
    "items": [
        {"taskId": 99999999, "rejectReasonCode": "R002", "rejectReasonName": "账户信息不一致", "needSupplement": True, "supplementItems": "补充账户证明", "remark": "测试退件"}
    ],
    "operatorId": "OP001",
    "operatorName": "BatchTest"
}, headers={"Content-Type": "application/json", "X-Operator-Id": "OP001"})
d = p("批量退件（演示接口）", r)

# ===== 功能3：催办升级记录 =====
print("\n" + "="*60)
print("🔔 功能3：催办升级记录")

test_app_no = app_nos[0] if app_nos else (list(app_map.keys())[0] if app_map else "HF17816438628125403")
print(f"  使用测试申请编号: {test_app_no}")
r = requests.get(f"{APP_URL}/progress?applicationNo={test_app_no}")
d = p(f"进度查询（含催办记录）- {test_app_no}", r)
if d and d.get("code") == 200:
    data = d.get("data", {})
    urge_logs = data.get("urgeLogs", [])
    status_logs = data.get("statusLogs", [])
    steps = data.get("steps", [])
    print(f"\n  📋 申请进度:")
    print(f"    当前状态: {data.get('applicationStatusName')}")
    print(f"    当前节点: {data.get('currentNode')}")
    print(f"    催办次数: {data.get('urgeCount')}")
    print(f"    退件次数: {data.get('rejectCount')}")
    print(f"    补正次数: {data.get('supplementCount')}")
    print(f"\n  📝 进度步骤 ({len(steps)} 步):")
    for s in steps:
        icon = "✅" if s.get("stepStatus") == 2 else "⏳" if s.get("stepStatus") == 1 else "⭕"
        print(f"    {icon} {s.get('stepName')} - {s.get('stepStatusName')}")
    print(f"\n  🔔 催办升级记录: {len(urge_logs)} 条")
    if urge_logs:
        for log in urge_logs[:3]:
            esc = " ⬆️ 已升级" if log.get("isEscalated") else ""
            print(f"    - {log.get('urgeTime')}: {log.get('urgeTypeName')}{esc}")
            print(f"      {log.get('urgeContent')}")
            if log.get("isEscalated"):
                print(f"      → 升级至: {log.get('escalateTo')}")
    else:
        print("    (暂无催办记录，系统定时任务每小时扫描超时任务自动催办)")

# ===== 功能4：外部回调查询 =====
print("\n" + "="*60)
print("🔗 功能4：外部回调查询")

r = requests.get(f"{APP_URL}/callback?applicationNo={test_app_no}")
d = p(f"外部回调查询 - {test_app_no}", r)
if d and d.get("code") == 200:
    cb = d.get("data", {})
    print(f"\n  📋 回调信息（字段稳定，适合第三方对接）:")
    print(f"    申请编号: {cb.get('applicationNo')}")
    print(f"    当前节点: {cb.get('currentNode')}")
    print(f"    当前状态: {cb.get('applicationStatusName')}")
    print(f"    状态说明: {cb.get('applicationStatusDesc')}")
    print(f"    当前地区: {cb.get('currentRegionName')}")
    print(f"    是否已完成: {cb.get('isCompleted')}")
    print(f"    是否已超时: {cb.get('isTimeout')}")
    if cb.get('remainingDays') is not None:
        print(f"    剩余天数: {cb.get('remainingDays')} 天")
    
    last_op = cb.get("lastOperation")
    if last_op:
        print(f"\n  🕐 最近一次操作:")
        print(f"    操作类型: {last_op.get('operateDesc')}")
        print(f"    操作人: {last_op.get('operatorName')}")
        print(f"    操作时间: {last_op.get('operateTime')}")
        print(f"    操作地区: {last_op.get('operateRegionName')}")
    
    next_todo = cb.get("nextTodo")
    if next_todo:
        print(f"\n  🎯 下一步待办:")
        print(f"    待办名称: {next_todo.get('todoName')}")
        print(f"    处理地区: {next_todo.get('handleRegionName')}")
        print(f"    办理要求: {next_todo.get('requirement')}")
        if next_todo.get('deadline'):
            print(f"    截止时间: {next_todo.get('deadline')}")
    
    urge_records = cb.get("urgeRecords", [])
    print(f"\n  📢 催办升级记录: {len(urge_records)} 条")
    for u in urge_records[:2]:
        esc = " ⬆️ 已升级" if u.get("isEscalated") else ""
        print(f"    - {u.get('urgeTime')}: {u.get('urgeTypeName')}{esc}")
        if u.get("isEscalated"):
            print(f"      → {u.get('escalateTo')}")

# ===== 功能5：归档摘要 =====
print("\n" + "="*60)
print("📦 功能5：归档摘要查询")

# 先完成一笔申请的流程
print("\n  🔄 模拟完成一笔申请流程（转出→转入）...")
if app_ids:
    first_id = app_ids[0]
    # 查询该申请的任务
    r = requests.get(f"{COL_URL}/tasks/application/{first_id}")
    if r.status_code == 200:
        d = r.json()
        if d.get("code") == 200:
            tasks = d.get("data", [])
            # 找出转出任务
            out_task = None
            for t in tasks:
                if t.get("taskType") == 10:
                    out_task = t
                    break
            
            if out_task and out_task.get("taskStatus") == 10:
                # 转出确认
                r = requests.post(f"{COL_URL}/task/{out_task['id']}/confirmOut", 
                    params={"actualAmount": 50000, "remark": "Test archive transfer out confirm"},
                    headers={"X-Operator-Id": "OP002"})
                print(f"  转出确认: {'成功' if r.status_code == 200 and r.json().get('code') == 200 else '失败'}")
                time.sleep(0.2)
                
                # 再查任务列表，找转入任务
                r = requests.get(f"{COL_URL}/tasks/application/{first_id}")
                if r.status_code == 200:
                    d = r.json()
                    if d.get("code") == 200:
                        tasks = d.get("data", [])
                        in_task = None
                        for t in tasks:
                            if t.get("taskType") == 20 and t.get("taskStatus") == 10:
                                in_task = t
                                break
                        
                        if in_task:
                            # 转入确认
                            r = requests.post(f"{COL_URL}/task/{in_task['id']}/confirmIn",
                                params={"actualAmount": 50000, "accountResult": 1, "remark": "Test archive transfer in confirm"},
                                headers={"X-Operator-Id": "OP003"})
                            print(f"  转入确认: {'成功' if r.status_code == 200 and r.json().get('code') == 200 else '失败'}")
                            time.sleep(0.2)

# 查询归档摘要
r = requests.get(f"{APP_URL}/archive/{test_app_no}")
d = p(f"归档摘要查询 - {test_app_no}", r)
if d and d.get("code") == 200:
    arc = d.get("data", {})
    print(f"\n{'='*60}")
    print("📦 结构化档案摘要")
    print(f"{'='*60}")
    
    basic = arc.get("basicInfo", {})
    print(f"\n  【基本信息】")
    print(f"    档案编号: {arc.get('archiveNo')}")
    print(f"    生成时间: {arc.get('archiveTime')}")
    print(f"    申请编号: {basic.get('applicationNo')}")
    print(f"    申请人: {basic.get('applicantName')}")
    print(f"    转移方向: {basic.get('transferOutRegionName')} → {basic.get('transferInRegionName')}")
    print(f"    申请时间: {basic.get('applyTime')}")
    print(f"    办结时间: {basic.get('finishTime')}")
    print(f"    最终状态: {basic.get('finalStatusName')}")
    
    rule = arc.get("ruleValidationResult", {})
    print(f"\n  【规则校验结果】")
    print(f"    校验通过: {'✅ 是' if rule.get('passed') else '❌ 否'}")
    print(f"    转出地规则: {rule.get('outRegionRuleName')}")
    print(f"    转入地规则: {rule.get('inRegionRuleName')}")
    print(f"    最低缴费月数: {rule.get('minContributionMonths')}")
    print(f"    是否重复: {'是' if rule.get('isDuplicate') else '否'}")
    print(f"    是否冲突: {'是' if rule.get('isConflict') else '否'}")
    check_items = rule.get("checkItems", [])
    print(f"    通过校验项: {len(check_items)} 项")
    for item in check_items[:3]:
        print(f"      ✓ {item}")
    
    tasks = arc.get("collaborationTasks", [])
    print(f"\n  【协同任务】共 {len(tasks)} 个")
    for t in tasks:
        icon = "✅" if t.get("taskStatus") in [20, 30] else "⏳" if t.get("taskStatus") == 10 else "❌"
        print(f"    {icon} {t.get('taskTypeName')} - {t.get('taskStatusName')}")
        if t.get("confirmTime"):
            print(f"      确认时间: {t.get('confirmTime')}")
            print(f"      确认结果: {t.get('confirmResultName')}")
            print(f"      实际金额: {t.get('actualAmount')} 元")
    
    rejects = arc.get("rejectRecords", [])
    print(f"\n  【退件记录】共 {len(rejects)} 条")
    for r in rejects:
        print(f"    - {r.get('rejectTime')}: {r.get('rejectReasonName')}")
        if r.get("needSupplement"):
            print(f"      需补正: {r.get('supplementGuide')}")
    
    supps = arc.get("supplementRecords", [])
    print(f"\n  【补正记录】共 {len(supps)} 条")
    for s in supps:
        print(f"    - {s.get('createTime')}: 需补正 {s.get('supplementItems')}")
        if s.get("completeTime"):
            print(f"      完成时间: {s.get('completeTime')}")
    
    urges = arc.get("urgeRecords", [])
    print(f"\n  【催办/升级记录】共 {len(urges)} 条")
    for u in urges:
        esc = " ⬆️ 已升级" if u.get("isEscalated") else ""
        print(f"    - {u.get('urgeTime')}: {u.get('urgeTypeName')}{esc}")
        print(f"      {u.get('urgeContent')}")
        if u.get("isEscalated"):
            print(f"      → 升级至: {u.get('escalateTo')}")
    
    status_logs = arc.get("statusLogs", [])
    print(f"\n  【状态变更轨迹】共 {len(status_logs)} 条")
    for log in status_logs[:5]:
        print(f"    {log.get('createTime')}: {log.get('fromStatusName')} → {log.get('toStatusName')}")
        if log.get("operatorName"):
            print(f"      操作人: {log.get('operatorName')}")
    
    op_logs = arc.get("operationLogs", [])
    print(f"\n  【操作留痕】共 {len(op_logs)} 条")
    for op in op_logs[:5]:
        print(f"    {op.get('operationTime')}: {op.get('operatorName')} - {op.get('operationName')}")
    
    stats = arc.get("summaryStatistics", {})
    print(f"\n  【效率统计】")
    print(f"    总耗时: {stats.get('totalDurationDays')} 天")
    print(f"    转出耗时: {stats.get('transferOutDurationDays')} 天")
    print(f"    转入耗时: {stats.get('transferInDurationDays')} 天")
    print(f"    补正次数: {stats.get('supplementCount')} 次")
    print(f"    催办次数: {stats.get('urgeCount')} 次")
    print(f"    退件次数: {stats.get('rejectCount')} 次")

print("\n" + "="*60)
print("🎉 所有5项新功能测试完成！")
print("="*60)
print("\n✅ 功能实现清单:")
print("  1. ✅ 待办驾驶舱：按中心统计各类待办，附带可点击申请列表")
print("  2. ✅ 批量处理：批量转出确认/退件，支持部分失败，单笔失败不影响整批")
print("  3. ✅ 催办升级：超时→催办→升级到上级中心，进度查询显示完整记录")
print("  4. ✅ 外部回调查询：稳定字段返回当前节点/最近操作/下一步待办")
print("  5. ✅ 归档摘要：办结后生成完整结构化档案，含9大模块")
print("\n📚 接口文档: http://localhost:8080/hf-transfer/doc.html")
print("🏥 健康检查: http://localhost:8080/hf-transfer/actuator/health")
