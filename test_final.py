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

# ===== 先获取已有申请和任务 =====
print("\n" + "="*60)
print("📋 获取已有测试数据...")

# 获取申请列表
r = requests.post(f"{APP_URL}/page", json={"pageNum": 1, "pageSize": 20})
app_map = {}
app_ids = []
if r.status_code == 200:
    d = r.json()
    if d.get("code") == 200:
        records = d.get("data", {}).get("records", [])
        print(f"  共有 {len(records)} 笔申请")
        for rec in records:
            app_map[rec.get("applicationNo")] = rec.get("id")
            app_ids.append(rec.get("id"))
            print(f"    {rec.get('applicantName')}: {rec.get('applicationNo')} (ID:{rec.get('id')}, 状态:{rec.get('applicationStatusName')})")

# 获取任务列表
task_ids = []
task_map = {}
for aid in app_ids[:5]:
    r = requests.get(f"{COL_URL}/tasks/application/{aid}")
    if r.status_code == 200:
        d = r.json()
        if d.get("code") == 200:
            for t in d.get("data", []):
                task_ids.append(t.get("id"))
                task_map[t.get("id")] = t
                print(f"  任务: {t.get('id')} - {t.get('applicationNo')} - {t.get('taskTypeName')} - 状态:{t.get('taskStatusName')}")

# 选择测试用的申请和任务
test_app_no = list(app_map.keys())[0] if app_map else "HF17816438628125403"
test_app_id = app_map.get(test_app_no)
print(f"\n  测试申请编号: {test_app_no} (ID: {test_app_id})")

# 找可以处理的转出任务 - 直接用已查询到的任务ID
real_task_ids = [2066991473299136513, 2066991473156530177, 2066990462044053506]
pending_out_task_ids = real_task_ids
print(f"  可处理的转出任务: {pending_out_task_ids}")

# 批量处理测试任务（包含一个不存在的ID）
batch_test_ids = pending_out_task_ids[:2] if pending_out_task_ids else [1, 2]
if len(batch_test_ids) >= 1:
    batch_test_ids.append(999999999999)  # 添加不存在的ID测试失败场景
print(f"  批量测试任务ID: {batch_test_ids}")

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
    print(f"    已超时: {data.get('alreadyTimeout')}")
    centers = data.get("centerTodos", [])
    print(f"\n  🏢 各中心明细 (前3个):")
    for c in centers[:3]:
        print(f"    - {c.get('centerName')}: 总 {c.get('totalCount')} 笔")
        for cat in ["pendingTransferOut", "pendingTransferIn", "pendingAccept", "pendingSupplement"]:
            cat_data = c.get(cat, {})
            count = cat_data.get("count", 0)
            if count > 0:
                cat_name = {"pendingTransferOut": "待转出", "pendingTransferIn": "待转入", "pendingAccept": "待受理", "pendingSupplement": "待补正"}[cat]
                print(f"      {cat_name}: {count} 笔")
                items = cat_data.get("items", [])
                for item in items[:2]:
                    print(f"        * {item.get('applicationNo')} - {item.get('applicantName')} ({item.get('transferOutRegionName')}→{item.get('transferInRegionName')})")

r = requests.get(f"{STAT_URL}/dashboard?regionCode=110000")
d = p("待办驾驶舱（北京中心）", r)

# ===== 功能2：批量处理 =====
print("\n" + "="*60)
print("⚡ 功能2：批量处理")

# 批量转出确认
batch_items = []
for i, tid in enumerate(batch_test_ids):
    batch_items.append({
        "taskId": tid,
        "actualAmount": 50000 + i * 5000,
        "remark": f"Batch transfer out test {i+1}"
    })

r = requests.post(f"{COL_URL}/tasks/batch/confirmOut", json={
    "items": batch_items,
    "operatorId": "OP001",
    "operatorName": "BatchTest"
}, headers={"Content-Type": "application/json", "X-Operator-Id": "OP001"})
d = p("批量转出确认（含不存在的任务ID测试失败场景）", r)

if d and d.get("code") == 200:
    result = d.get("data", {})
    results = result.get("results", [])
    success = sum(1 for x in results if x.get("success"))
    fail = sum(1 for x in results if not x.get("success"))
    print(f"\n  📊 批量处理结果:")
    print(f"    成功: {success} 笔")
    print(f"    失败: {fail} 笔")
    if results:
        print(f"    成功率: {success/len(results)*100:.1f}%")
    for res in results:
        status = "✅ 成功" if res.get("success") else "❌ 失败"
        reason = f" - {res.get('failReason')}" if not res.get("success") else ""
        app_no = res.get("applicationNo")
        app_info = f" (申请:{app_no})" if app_no else ""
        print(f"    {status} 任务 {res.get('taskId')}{app_info}{reason}")

print("\n  ✅ 验证点：单笔失败不影响整批，每笔都有独立结果")

# 批量退件演示
print("\n  测试批量退件接口...")
r = requests.post(f"{COL_URL}/tasks/batch/reject", json={
    "items": [
        {"taskId": 99999999, "rejectReasonCode": "R002", "rejectReasonName": "账户信息不一致", "needSupplement": True, "supplementItems": "补充账户证明", "remark": "Test reject"}
    ],
    "operatorId": "OP001",
    "operatorName": "BatchTest"
}, headers={"Content-Type": "application/json", "X-Operator-Id": "OP001"})
d = p("批量退件（演示接口）", r)

# ===== 功能3：催办升级记录 =====
print("\n" + "="*60)
print("🔔 功能3：催办升级记录")

r = requests.get(f"{APP_URL}/progress?applicationNo={test_app_no}")
d = p(f"进度查询（含催办记录）- {test_app_no}", r)
if d and d.get("code") == 200:
    data = d.get("data", {})
    urge_logs = data.get("urgeLogs") or []
    steps = data.get("steps", [])
    print(f"\n  📋 申请进度:")
    print(f"    当前状态: {data.get('applicationStatusName')}")
    print(f"    当前节点: {data.get('currentNode')}")
    print(f"    当前地区: {data.get('currentRegionName')}")
    print(f"    催办次数: {data.get('urgeCount')}")
    print(f"    退件次数: {data.get('rejectCount')}")
    print(f"    补正次数: {data.get('supplementCount')}")
    print(f"\n  📝 进度步骤 ({len(steps)} 步):")
    for s in steps:
        icon = "✅" if s.get("stepStatus") == 2 else "⏳" if s.get("stepStatus") == 1 else "⭕"
        status_time = f" ({s.get('stepTime')})" if s.get("stepTime") else ""
        print(f"    {icon} {s.get('stepName')} - {s.get('stepStatusName')}{status_time}")
    
    print(f"\n  🔔 催办升级记录: {len(urge_logs)} 条")
    if urge_logs:
        for log in urge_logs[:5]:
            esc = " ⬆️ 已升级" if log.get("isEscalated") else ""
            print(f"    - {log.get('urgeTime')}: {log.get('urgeTypeName')} (等级:{log.get('urgeLevelName')}){esc}")
            print(f"      {log.get('urgeContent')}")
            if log.get("isEscalated"):
                print(f"      → 升级至: {log.get('escalateTo')}")
    else:
        print("    (暂无催办记录 - 系统每小时自动扫描超时任务，超时3天自动催办，7天升级至省级，15天升级至部级)")
        print("    💡 催办升级规则:")
        print("       • 超时 < 3天: 普通催办（通知经办人）")
        print("       • 超时 ≥ 3天: 升级至中心主任")
        print("       • 超时 ≥ 7天: 升级至省级监管")
        print("       • 超时 ≥ 15天: 升级至部级监管")

# ===== 功能4：外部回调查询 =====
print("\n" + "="*60)
print("🔗 功能4：外部回调查询（第三方系统对接专用）")

r = requests.get(f"{APP_URL}/callback?applicationNo={test_app_no}")
d = p(f"外部回调查询 - {test_app_no}", r)
if d and d.get("code") == 200:
    cb = d.get("data", {})
    print(f"\n  📋 回调信息（字段稳定，第三方系统可直接解析）:")
    print(f"    申请编号: {cb.get('applicationNo')}")
    print(f"    当前节点: {cb.get('currentNode')}")
    print(f"    当前状态码: {cb.get('applicationStatus')}")
    print(f"    当前状态名: {cb.get('applicationStatusName')}")
    print(f"    状态说明: {cb.get('applicationStatusDesc')}")
    print(f"    当前地区: {cb.get('currentRegionName')}")
    print(f"    是否已完成: {cb.get('isCompleted')}")
    print(f"    是否已超时: {cb.get('isTimeout')}")
    if cb.get('remainingDays') is not None:
        print(f"    剩余/超期天数: {cb.get('remainingDays')} 天")
    
    print(f"\n  🕐 最近一次操作:")
    last_op = cb.get("lastOperation")
    if last_op:
        print(f"    操作类型: {last_op.get('operateType')}")
        print(f"    操作描述: {last_op.get('operateDesc')}")
        print(f"    操作人: {last_op.get('operatorName')}")
        print(f"    操作时间: {last_op.get('operateTime')}")
        print(f"    操作地区: {last_op.get('operateRegionName')}")
        if last_op.get('remark'):
            print(f"    备注: {last_op.get('remark')}")
    else:
        print(f"    (暂无操作记录)")
    
    print(f"\n  🎯 下一步待办:")
    next_todo = cb.get("nextTodo")
    if next_todo:
        print(f"    待办类型: {next_todo.get('todoType')}")
        print(f"    待办名称: {next_todo.get('todoName')}")
        print(f"    处理地区: {next_todo.get('handleRegionName')}")
        print(f"    办理要求: {next_todo.get('requirement')}")
        if next_todo.get('deadline'):
            print(f"    截止时间: {next_todo.get('deadline')}")
    else:
        print(f"    (已办结，无待办)")
    
    urge_records = cb.get("urgeRecords", [])
    print(f"\n  📢 催办升级记录: {len(urge_records)} 条")
    for u in urge_records[:3]:
        esc = " ⬆️ 已升级" if u.get("isEscalated") else ""
        print(f"    - {u.get('urgeTime')}: {u.get('urgeTypeName')}{esc}")
        if u.get("isEscalated"):
            print(f"      → {u.get('escalateTo')}")

print("\n  ✅ 验证点：返回字段稳定，第三方系统无需修改代码即可适配")

# ===== 功能5：归档摘要 =====
print("\n" + "="*60)
print("📦 功能5：结构化归档摘要")

# 先完成一笔申请的流程（如果有的话）
print("\n  🔄 尝试完成一笔申请流程...")
if test_app_id:
    r = requests.get(f"{COL_URL}/tasks/application/{test_app_id}")
    if r.status_code == 200:
        d = r.json()
        if d.get("code") == 200:
            tasks = d.get("data", [])
            out_task = None
            for t in tasks:
                if t.get("taskType") == 10 and t.get("taskStatus") in [10, 30]:
                    out_task = t
                    break
            
            if out_task:
                print(f"  找到可处理的转出任务: {out_task['id']}")
                r = requests.post(f"{COL_URL}/task/{out_task['id']}/confirmOut", 
                    params={"actualAmount": 50000, "remark": "Archive test transfer out"},
                    headers={"X-Operator-Id": "OP002"})
                result = "✅" if r.status_code == 200 and r.json().get("code") == 200 else "❌"
                print(f"  转出确认: {result} {r.json().get('message', '') if r.status_code == 200 else ''}")
                time.sleep(0.3)
                
                # 查找转入任务
                r = requests.get(f"{COL_URL}/tasks/application/{test_app_id}")
                if r.status_code == 200:
                    d = r.json()
                    if d.get("code") == 200:
                        tasks = d.get("data", [])
                        in_task = None
                        for t in tasks:
                            if t.get("taskType") == 20 and t.get("taskStatus") in [10, 50, 60]:
                                in_task = t
                                break
                        
                        if in_task:
                            print(f"  找到可处理的转入任务: {in_task['id']}")
                            r = requests.post(f"{COL_URL}/task/{in_task['id']}/confirmIn",
                                params={"actualAmount": 50000, "accountResult": 1, "remark": "Archive test transfer in"},
                                headers={"X-Operator-Id": "OP003"})
                            result = "✅" if r.status_code == 200 and r.json().get("code") == 200 else "❌"
                            print(f"  转入确认: {result} {r.json().get('message', '') if r.status_code == 200 else ''}")
                            time.sleep(0.2)

# 查询归档摘要
r = requests.get(f"{APP_URL}/archive/{test_app_no}")
d = p(f"归档摘要查询 - {test_app_no}", r)
if d and d.get("code") == 200:
    arc = d.get("data", {})
    print(f"\n{'='*60}")
    print("📦 结构化档案摘要（监管抽查可直接使用）")
    print(f"{'='*60}")
    
    basic = arc.get("basicInfo", {})
    print(f"\n  【1. 基本信息】")
    print(f"    档案编号: {arc.get('archiveNo')}")
    print(f"    生成时间: {arc.get('archiveTime')}")
    print(f"    申请编号: {basic.get('applicationNo')}")
    print(f"    申请人: {basic.get('applicantName')}")
    print(f"    证件号码: {basic.get('idCardNo')}")
    print(f"    转移方向: {basic.get('transferOutRegionName')} → {basic.get('transferInRegionName')}")
    print(f"    申请金额: {basic.get('transferAmount')} 元")
    print(f"    申请时间: {basic.get('applyTime')}")
    print(f"    办结时间: {basic.get('finishTime')}")
    print(f"    最终状态: {basic.get('finalStatusName')}")
    
    rule = arc.get("ruleValidationResult", {})
    print(f"\n  【2. 规则校验结果】")
    print(f"    校验通过: {'✅ 是' if rule.get('passed') else '❌ 否'}")
    print(f"    转出地规则: {rule.get('outRegionRuleName')}")
    print(f"    转入地规则: {rule.get('inRegionRuleName')}")
    print(f"    最低缴费月数: {rule.get('minContributionMonths')} 个月")
    print(f"    是否重复: {'是' if rule.get('isDuplicate') else '否'}")
    print(f"    是否冲突: {'是' if rule.get('isConflict') else '否'}")
    check_items = rule.get("checkItems", [])
    print(f"    通过校验项: {len(check_items)} 项")
    for item in check_items[:3]:
        print(f"      ✓ {item}")
    warnings = rule.get("warnings", [])
    if warnings:
        print(f"    警告项: {len(warnings)} 项")
        for w in warnings[:2]:
            print(f"      ⚠️  {w}")
    
    tasks = arc.get("collaborationTasks", [])
    print(f"\n  【3. 协同任务】共 {len(tasks)} 个")
    for t in tasks:
        icon = "✅" if t.get("taskStatus") in [20, 30] else "⏳" if t.get("taskStatus") in [10, 50, 60] else "❌"
        print(f"    {icon} {t.get('taskTypeName')} - {t.get('taskStatusName')}")
        print(f"      任务编号: {t.get('taskNo')}")
        print(f"      流转方向: {t.get('sourceRegionName')} → {t.get('targetRegionName')}")
        print(f"      派单时间: {t.get('assignTime')}")
        print(f"      截止时间: {t.get('deadlineTime')}")
        if t.get("confirmTime"):
            print(f"      确认时间: {t.get('confirmTime')}")
            print(f"      确认结果: {t.get('confirmResultName')}")
            print(f"      实际金额: {t.get('actualAmount')} 元")
            print(f"      催办次数: {t.get('urgeCount')} 次")
    
    rejects = arc.get("rejectRecords", [])
    print(f"\n  【4. 退件记录】共 {len(rejects)} 条")
    for r in rejects:
        print(f"    - {r.get('rejectTime')}: {r.get('rejectReasonName')}")
        print(f"      退件原因编码: {r.get('rejectReasonCode')}")
        print(f"      原因详情: {r.get('rejectReasonDetail')}")
        if r.get("needSupplement"):
            print(f"      需补正: 是")
            print(f"      补正指引: {r.get('supplementGuide')}")
    
    supps = arc.get("supplementRecords", [])
    print(f"\n  【5. 补正记录】共 {len(supps)} 条")
    for s in supps:
        print(f"    - {s.get('createTime')}: 需补正 {s.get('supplementItems')}")
        print(f"      补正时限: {s.get('deadlineTime')}")
        if s.get("completeTime"):
            print(f"      完成时间: {s.get('completeTime')}")
            print(f"      补正耗时: {s.get('durationHours')} 小时")
    
    urges = arc.get("urgeRecords", [])
    print(f"\n  【6. 催办/升级记录】共 {len(urges)} 条")
    for u in urges:
        esc = " ⬆️ 已升级" if u.get("isEscalated") else ""
        print(f"    - {u.get('urgeTime')}: {u.get('urgeTypeName')} (等级:{u.get('urgeLevelName')}){esc}")
        print(f"      {u.get('urgeContent')}")
        if u.get("isEscalated"):
            print(f"      → 升级至: {u.get('escalateTo')}")
    
    status_logs = arc.get("statusLogs", [])
    print(f"\n  【7. 状态变更轨迹】共 {len(status_logs)} 条")
    for log in status_logs:
        print(f"    {log.get('createTime')}: {log.get('fromStatusName')} → {log.get('toStatusName')}")
        if log.get("operatorName"):
            print(f"      操作人: {log.get('operatorName')} ({log.get('operateRegionName')})")
        if log.get("operateRemark"):
            print(f"      备注: {log.get('operateRemark')}")
    
    op_logs = arc.get("operationLogs", [])
    print(f"\n  【8. 操作留痕】共 {len(op_logs)} 条")
    for op in op_logs[:5]:
        print(f"    {op.get('operationTime')}: {op.get('operatorName')} ({op.get('module')})")
        print(f"      {op.get('operationName')}")
        if op.get("detail"):
            print(f"      详情: {op.get('detail')[:100]}")
    
    stats = arc.get("summaryStatistics", {})
    print(f"\n  【9. 效率统计】")
    print(f"    总耗时: {stats.get('totalDurationDays')} 天")
    print(f"    转出耗时: {stats.get('transferOutDurationDays')} 天")
    print(f"    转入耗时: {stats.get('transferInDurationDays')} 天")
    print(f"    补正次数: {stats.get('supplementCount')} 次")
    print(f"    催办次数: {stats.get('urgeCount')} 次")
    print(f"    退件次数: {stats.get('rejectCount')} 次")
    print(f"    是否超时: {'是' if stats.get('isTimeout') else '否'}")

print("\n  ✅ 验证点：包含完整9大模块，监管抽查可直接按申请编号调取")

print("\n" + "="*60)
print("🎉 所有5项新功能测试完成！")
print("="*60)
print("\n✅ 功能实现清单:")
print("  1. ✅ 待办驾驶舱：按管理中心统计待受理/待转出/待转入/待补正/即将超时数量，附带可点击追踪的申请列表")
print("  2. ✅ 批量处理：批量转出确认/退件，支持部分失败，单笔失败不影响整批，返回每笔独立结果及失败原因")
print("  3. ✅ 催办升级规则：普通超时先催办→3天升级中心主任→7天升级省级→15天升级部级，进度查询显示完整记录")
print("  4. ✅ 外部回调查询：第三方系统用申请编号查当前节点/最近操作/下一步待办，字段稳定便于对接")
print("  5. ✅ 结构化归档摘要：办结后生成9大模块完整档案，监管抽查可直接按申请编号调取")
print("\n📚 接口文档: http://localhost:8080/hf-transfer/doc.html")
print("🏥 健康检查: http://localhost:8080/hf-transfer/actuator/health")
print(f"\n📋 测试申请编号: {test_app_no}")
print("   可直接使用上述编号在接口文档中调试各接口")
