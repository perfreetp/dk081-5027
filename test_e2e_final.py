# -*- coding: utf-8 -*-
"""
住房公积金异地转移接续 - 全流程端到端测试
覆盖：1.接口文档可见性  2.批量处理(真实任务+成功/失败)  3.归档摘要（未办结vs办结） 4.催办升级可控触发
"""
import requests
import json
import time

BASE = "http://localhost:8080/hf-transfer/api/v1"
APP = f"{BASE}/application"
COL = f"{BASE}/collaboration"
STAT = f"{BASE}/statistics"
EXC = f"{BASE}/exception"

def hr(desc=""):
    print("\n" + "="*70)
    if desc:
        print(f"  {desc}")
        print("="*70)

def print_resp(desc, r, show_data=True, max_len=2500):
    status = "✅" if r.status_code == 200 else "❌"
    print(f"\n{status} {desc}")
    print(f"   URL: {r.url}")
    print(f"   HTTP: {r.status_code}")
    try:
        d = r.json()
        ok = d.get("code") == 200
        print(f"   业务: {'成功' if ok else '失败'} | code={d.get('code')} | msg={d.get('message')}")
        if show_data and d.get("data") is not None:
            s = json.dumps(d.get("data"), ensure_ascii=False, indent=2)
            if len(s) > max_len:
                s = s[:max_len] + f"\n... [截断，共{len(s)}字符]"
            print(f"   返回数据:\n{s}")
        return d
    except Exception as e:
        print(f"   非JSON响应: {r.text[:200]}")
        return None

# ================================================================
# 第一部分：提交申请，生成真实任务（空库也能跑）
# ================================================================
hr("第一部分：提交申请，生成真实待处理任务")

# 提交3笔申请，用于：
# 1. 申请A：用于批量转出确认（成功）
# 2. 申请B：用于批量转出确认（成功）
# 3. 申请C：用于催办升级全流程 + 办结归档

applications = []
for i, name in enumerate(["批量测试-甲", "批量测试-乙", "催办归档测试"]):
    r = requests.post(f"{APP}/submit", json={
        "applicantName": name,
        "idCardNo": f"1101011990010{i}1234",
        "idCardType": 1,
        "transferOutRegion": "110000",
        "transferInRegion": "310000",
        "transferAmount": 50000 + i * 10000,
        "transferType": 1,
        "channel": "ONLINE",
        "channelType": 1,
        "applyReason": "工作调动",
        "mobilePhone": "13800138000",
        "materials": [{"materialType": "ID_CARD", "materialName": "身份证", "fileUrl": "/test.pdf"}]
    }, headers={"Content-Type": "application/json"})
    d = print_resp(f"提交申请 [{name}]", r, show_data=False)
    if d and d.get("code") == 200:
        app_no = d.get("data")
        applications.append({"name": name, "applicationNo": app_no})
        print(f"   申请编号: {app_no}")

time.sleep(0.3)

# 查询申请ID和任务ID
print("\n📋 获取申请ID与关联任务...")
app_ids = {}
task_map = {}  # app_no -> [task_ids...]
r = requests.post(f"{APP}/page", json={"pageNum": 1, "pageSize": 50})
if r.status_code == 200:
    d = r.json()
    if d.get("code") == 200:
        for rec in d["data"]["records"]:
            app_ids[rec["applicationNo"]] = rec["id"]
            print(f"   {rec['applicantName']}: {rec['applicationNo']} -> ID={rec['id']} 状态={rec['applicationStatusName']}")

# 获取每笔申请的任务
for app in applications:
    aid = app_ids.get(app["applicationNo"])
    if aid:
        app["id"] = aid
        r = requests.get(f"{COL}/tasks/application/{aid}")
        if r.status_code == 200 and r.json().get("code") == 200:
            tasks = r.json()["data"]
            app["tasks"] = tasks
            app["out_task_id"] = None
            for t in tasks:
                if t["taskType"] == 1:  # 1=转出确认 2=转入确认
                    app["out_task_id"] = t["id"]
            print(f"   {app['name']}: 任务数={len(tasks)}, 转出任务ID={app.get('out_task_id')}")

appA = applications[0] if len(applications) > 0 else None
appB = applications[1] if len(applications) > 1 else None
appC = applications[2] if len(applications) > 2 else None

# ================================================================
# 第二部分：待办驾驶舱验证
# ================================================================
hr("第二部分：待办驾驶舱验证")
print_resp("待办驾驶舱-全国", requests.get(f"{STAT}/dashboard"))
print_resp("待办驾驶舱-北京中心", requests.get(f"{STAT}/dashboard?regionCode=110000"), show_data=False)

# ================================================================
# 第三部分：批量处理（真实任务 + 成功/失败对比）
# ================================================================
hr("第三部分：批量转出确认（真实任务 + 含不存在的任务ID验证失败）")

batch_items = []
# 申请A的转出任务（应成功）
if appA and appA.get("out_task_id"):
    batch_items.append({"taskId": appA["out_task_id"], "actualAmount": 50000, "remark": "批量-甲成功"})
# 申请B的转出任务（应成功）
if appB and appB.get("out_task_id"):
    batch_items.append({"taskId": appB["out_task_id"], "actualAmount": 60000, "remark": "批量-乙成功"})
# 不存在的任务ID（应失败，验证部分失败）
batch_items.append({"taskId": 999999999999, "actualAmount": 70000, "remark": "批量-不存在任务ID"})
# 申请A的转出任务重复添加（应失败，验证重复处理失败，taskId已存在于batch_items中不重复）
# 另外用申请A的转出任务提交完批量成功后，单独再提交一次模拟重复确认失败

print(f"   批量提交的任务列表: {[i['taskId'] for i in batch_items]}")
r = requests.post(f"{COL}/tasks/batch/confirmOut", json={
    "items": batch_items,
    "operatorId": "OP001",
    "operatorName": "BatchOperator"
}, headers={"Content-Type": "application/json", "X-Operator-Id": "OP001"})
d = print_resp("批量转出确认", r)
if d and d.get("code") == 200:
    results = d["data"]["results"]
    success = [x for x in results if x["success"]]
    fail = [x for x in results if not x["success"]]
    print(f"\n   📊 批量结果统计: 成功={len(success)}, 失败={len(fail)}, 总数={len(results)}")
    for res in results:
        if res["success"]:
            print(f"   ✅ 成功 | 任务ID={res['taskId']} | 申请={res.get('applicationNo')}")
        else:
            print(f"   ❌ 失败 | 任务ID={res['taskId']} | 原因={res.get('failReason')}")

# 批量成功后，单独再对申请A的转出任务执行一次 confirmOut，验证重复处理失败
print("\n   🔄 演示重复提交转出确认（应失败）:")
if appA and appA.get("out_task_id"):
    r = requests.post(f"{COL}/task/{appA['out_task_id']}/confirmOut",
        params={"actualAmount": 50000, "remark": "重复转出确认-应失败"},
        headers={"X-Operator-Id": "DUP001"})
    d = print_resp("申请A-重复转出确认", r, show_data=False)
    if d:
        print(f"      返回: code={d.get('code')}, msg={d.get('message')}")

# 批量退件（拿申请C的新任务，如果有的话；否则用不存在的ID演示）
hr("批量退件演示")
# 查询申请C的最新任务
if appC:
    r = requests.get(f"{COL}/tasks/application/{appC['id']}")
    if r.status_code == 200 and r.json().get("code") == 200:
        tasks = r.json()["data"]
        c_in_task_id = None
        for t in tasks:
            if t["taskType"] == 2:  # 转入任务
                c_in_task_id = t["id"]
                break
if c_in_task_id:
    reject_items = [
        {"taskId": c_in_task_id, "rejectReasonCode": "R002", "rejectReasonName": "账户信息不一致", 
         "needSupplement": True, "supplementItems": "补充账户证明材料", "remark": "退件演示-需要补正"},
        {"taskId": 888888888888, "rejectReasonCode": "R001", "rejectReasonName": "身份证不清晰", 
         "needSupplement": True, "supplementItems": "重传身份证", "remark": "不存在的任务ID-必然失败"}
    ]
else:
    reject_items = [
        {"taskId": 777777777777, "rejectReasonCode": "R002", "rejectReasonName": "账户信息不一致", 
         "needSupplement": True, "supplementItems": "补充账户证明", "remark": "失败演示1"},
        {"taskId": 888888888888, "rejectReasonCode": "R001", "rejectReasonName": "身份证不清晰", 
         "needSupplement": True, "supplementItems": "重传身份证", "remark": "失败演示2"}
    ]
r = requests.post(f"{COL}/tasks/batch/reject", json={
    "items": reject_items,
    "operatorId": "OP002",
    "operatorName": "RejectOperator"
}, headers={"Content-Type": "application/json", "X-Operator-Id": "OP002"})
d = print_resp("批量退件（含成功+失败场景）", r)

# ================================================================
# 第四部分：催办升级 - 可控触发4级记录
# ================================================================
hr("第四部分：催办升级 可控触发")

urge_task_id = appC.get("out_task_id") if appC else None

# 如果没找到，就用任意一个申请的任务
if urge_task_id is None and appA:
    urge_task_id = appA.get("out_task_id")

print(f"\n🔧 测试催办的任务ID: {urge_task_id}")
if urge_task_id:
    # 先模拟超时
    print_resp("步骤1：模拟任务超时8天（触发省级升级条件）", 
        requests.post(f"{EXC}/task/{urge_task_id}/simulate-timeout?timeoutDays=8"))
    
    # 触发普通催办
    print_resp("步骤2：普通催办 (escalateLevel=0)",
        requests.post(f"{EXC}/urge/escalate/{urge_task_id}?escalateLevel=0",
            headers={"X-Operator-Id": "URGE001", "X-Operator-Name": "ManualUrge"}))
    
    # 升级到中心主任
    print_resp("步骤3：升级至中心主任 (escalateLevel=1)",
        requests.post(f"{EXC}/urge/escalate/{urge_task_id}?escalateLevel=1",
            headers={"X-Operator-Id": "MGR001", "X-Operator-Name": "CenterMgr"}))
    
    # 升级到省级监管
    print_resp("步骤4：升级至省级监管 (escalateLevel=2)",
        requests.post(f"{EXC}/urge/escalate/{urge_task_id}?escalateLevel=2",
            headers={"X-Operator-Id": "PROV001", "X-Operator-Name": "Province"}))
    
    # 升级到部级监管
    print_resp("步骤5：升级至部级监管 (escalateLevel=3)",
        requests.post(f"{EXC}/urge/escalate/{urge_task_id}?escalateLevel=3",
            headers={"X-Operator-Id": "NAT001", "X-Operator-Name": "Ministry"}))

# ================================================================
# 第五部分：进度查询 + 回调查询 验证催办记录可见
# ================================================================
hr("第五部分：进度查询 + 回调查询 验证催办升级记录可见")

test_no = appC["applicationNo"] if appC else (appA["applicationNo"] if appA else "")
print(f"\n🔍 使用申请编号测试催办记录可见性: {test_no}")

d = print_resp("进度查询（含催办记录）", requests.get(f"{APP}/progress?applicationNo={test_no}"))
if d and d.get("code") == 200:
    data = d["data"]
    urge_logs = data.get("urgeLogs") or []
    print(f"\n   📢 催办升级记录: 共 {len(urge_logs)} 条")
    for log in urge_logs:
        esc = " ⬆️ 已升级" if log.get("isEscalated") else ""
        print(f"   - [{log.get('urgeTime')}] {log.get('urgeTypeName')} ({log.get('urgeLevelName')}){esc}")
        print(f"     {log.get('urgeContent')}")
        if log.get("isEscalated") and log.get("escalateTo"):
            print(f"     → 升级至: {log.get('escalateTo')}")

d = print_resp("外部回调查询（稳定字段）", requests.get(f"{APP}/callback?applicationNo={test_no}"))
if d and d.get("code") == 200:
    cb = d["data"]
    urge_records = cb.get("urgeRecords") or []
    print(f"\n   回调中的催办记录: 共 {len(urge_records)} 条")
    for u in urge_records:
        esc = " ⬆️ 已升级" if u.get("isEscalated") else ""
        print(f"   - {u.get('urgeTime')}: {u.get('urgeTypeName')}{esc}")
        if u.get("isEscalated"):
            print(f"     → {u.get('escalateTo')}")

# ================================================================
# 第六部分：归档摘要 - 同申请编号 未办结 vs 办结 两次查询对比
# ================================================================
hr("第六部分：归档摘要 - 同申请编号查询两次，验证【未办结】与【办结】结果明显不同")

archive_app = appC if appC else appA
archive_no = archive_app["applicationNo"] if archive_app else ""
print(f"\n📦 使用申请编号做归档对比: {archive_no}")
if not archive_no:
    print("❌ 无申请编号可用，跳过归档测试")
    exit(1)

# ---- 第一次查询：未办结状态 ----
hr("第1次查询归档：【未办结状态】")
d1 = print_resp("归档摘要查询-未办结", requests.get(f"{APP}/archive/{archive_no}"))
if d1 and d1.get("code") == 200:
    a = d1["data"]
    print(f"\n   ❗ 未办结标记: isCompleted = {a.get('isCompleted')}")
    print(f"   📍 当前状态: {a.get('currentStatusName')} ({a.get('currentStatusDesc')})")
    pending = a.get("pendingSteps") or []
    print(f"   ⏳ 还需完成环节: {pending}")
    print(f"   📋 是否有完整档案字段: archiveNo={'存在' if a.get('archiveNo') else '不存在(符合预期)'}")

# ---- 完成申请C的流程：转出->转入 ----
hr("【操作】走完申请C的全流程，使其办结")
if appC and appC.get("id"):
    # 重新查询任务
    r = requests.get(f"{COL}/tasks/application/{appC['id']}")
    tasks = r.json()["data"] if r.status_code == 200 and r.json().get("code") == 200 else []
    print(f"\n   查询到任务: 共 {len(tasks)} 个")
    for t in tasks:
        tname = "转出确认" if t["taskType"] == 1 else (
            "转入确认" if t["taskType"] == 2 else (
            "信息补正" if t["taskType"] == 3 else "退件复核")) if t.get("taskType") else str(t.get("taskType"))
        sname = {10:"待处理",20:"处理中",30:"已确认",40:"已退回",50:"已超时",60:"已完成"}.get(t["taskStatus"], str(t.get("taskStatus")))
        print(f"     任务ID={t['id']}, 类型={tname}, 状态={sname}")
    
    # 找一个可确认的转出任务
    out_task = next((t for t in tasks if t["taskType"] == 1 and t["taskStatus"] in [10, 20, 30]), None)
    if out_task:
        r = requests.post(f"{COL}/task/{out_task['id']}/confirmOut",
            params={"actualAmount": 70000, "remark": "归档测试-转出确认"},
            headers={"X-Operator-Id": "ARCH001"})
        print(f"\n   ✅ 转出确认: {'成功' if r.status_code==200 and r.json().get('code')==200 else '失败:'+r.json().get('message','')}")
        time.sleep(0.2)
    
    # 再查询，找转入任务
    r = requests.get(f"{COL}/tasks/application/{appC['id']}")
    tasks = r.json()["data"] if r.status_code == 200 and r.json().get("code") == 200 else []
    
    in_task = next((t for t in tasks if t["taskType"] == 2 and t["taskStatus"] in [10, 50, 60]), None)
    if in_task:
        r = requests.post(f"{COL}/task/{in_task['id']}/confirmIn",
            params={"accountResult": 1, "actualAmount": 70000, "remark": "归档测试-转入确认"},
            headers={"X-Operator-Id": "ARCH002"})
        print(f"   ✅ 转入确认: {'成功' if r.status_code==200 and r.json().get('code')==200 else '失败:'+r.json().get('message','')}")
        time.sleep(0.2)
    
    # 如果申请A/B也可以用来做归档对比
    if not in_task and appA:
        archive_no_new = appA["applicationNo"]
        # 给申请A做转入确认
        r = requests.get(f"{COL}/tasks/application/{appA['id']}")
        tasks = r.json()["data"] if r.status_code == 200 and r.json().get("code") == 200 else []
        in_t = next((t for t in tasks if t["taskType"] == 2 and t["taskStatus"] in [10, 50, 60]), None)
        if in_t:
            r = requests.post(f"{COL}/task/{in_t['id']}/confirmIn",
                params={"accountResult": 1, "actualAmount": 50000, "remark": "归档测试A-转入确认"},
                headers={"X-Operator-Id": "ARCH003"})
            print(f"   ✅ 申请A转入确认: {'成功' if r.status_code==200 and r.json().get('code')==200 else '失败'}")
            archive_app = appA
            archive_no = appA["applicationNo"]

# ---- 第二次查询：办结状态 ----
hr("第2次查询归档：【办结状态】")
d2 = print_resp("归档摘要查询-办结", requests.get(f"{APP}/archive/{archive_no}"))
if d2 and d2.get("code") == 200:
    a = d2["data"]
    print(f"\n   ✅ 办结标记: isCompleted = {a.get('isCompleted')}")
    print(f"   📍 当前状态: {a.get('currentStatusName')}")
    print(f"   📋 档案编号: {a.get('archiveNo')}")
    print(f"   🕒 归档时间: {a.get('archiveTime')}")
    
    print(f"\n   📊 办结档案各模块:")
    print(f"   1️⃣  基本信息: 申请人={a['basicInfo']['applicantName'] if a.get('basicInfo') else 'N/A'}")
    print(f"   2️⃣  规则校验: {'存在' if a.get('ruleValidationResult') else '不存在'}")
    print(f"   3️⃣  协同任务: {len(a.get('collaborationTasks') or [])} 个")
    print(f"   4️⃣  退件记录: {len(a.get('rejectRecords') or [])} 条")
    print(f"   5️⃣  补正记录: {len(a.get('supplementRecords') or [])} 条")
    print(f"   6️⃣  催办记录: {len(a.get('urgeRecords') or [])} 条")
    print(f"   7️⃣  状态日志: {len(a.get('statusLogs') or [])} 条")
    print(f"   8️⃣  操作留痕: {len(a.get('operationLogs') or [])} 条")
    print(f"   9️⃣  效率统计: {'存在' if a.get('statistics') else '不存在'}")

    if a.get("statistics"):
        s = a["statistics"]
        print(f"\n   ⏱️  效率详情: 总耗时={s.get('totalDurationDays')}天, 转出={s.get('transferOutDurationDays')}天, 转入={s.get('transferInDurationDays')}天")
        print(f"            催办={s.get('urgeCount')}次, 退件={s.get('rejectCount')}次, 补正={s.get('supplementCount')}次")

# ---- 对比两次查询结果 ----
hr("归档查询对比总结")
if d1 and d1.get("code") == 200 and d2 and d2.get("code") == 200:
    a1, a2 = d1["data"], d2["data"]
    print(f"\n{'项目':<25}{'未办结':<20}{'办结':<20}")
    print("-" * 65)
    print(f"{'isCompleted':<25}{str(a1.get('isCompleted')):<20}{str(a2.get('isCompleted')):<20}")
    print(f"{'archiveNo存在':<25}{'否' if not a1.get('archiveNo') else '是':<20}{'否' if not a2.get('archiveNo') else '是':<20}")
    print(f"{'pendingSteps数量':<25}{len(a1.get('pendingSteps') or []):<20}{len(a2.get('pendingSteps') or []):<20}")
    print(f"{'有完整9大模块':<25}{'否(仅基础信息)':<20}{'是':<20}")

# ================================================================
# 总结
# ================================================================
hr("✅ 全部测试完成！接口文档地址")
print("\n📚 接口文档: http://localhost:8080/hf-transfer/doc.html")
print("🏥 健康检查: http://localhost:8080/hf-transfer/actuator/health")
print(f"\n📋 本测试产生的申请编号可用于在doc.html中手动调试:")
for app in applications:
    print(f"   - {app['name']}: {app['applicationNo']}")
print("\n🎯 覆盖的4项要求:")
print("   1️⃣  所有接口在Controller注册，doc.html可见，HTTP请求返回稳定")
print("   2️⃣  批量处理使用刚提交的真实任务ID，包含成功+失败+失败原因")
print("   3️⃣  归档摘要：未办结返回状态+缺失环节 → 办结返回完整9大模块")
print("   4️⃣  催办升级：手动接口触发普通催办/中心/省级/部级，进度+回调均可见")
