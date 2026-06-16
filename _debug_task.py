import requests, json
BASE = "http://localhost:8080/hf-transfer/api/v1"
r = requests.post(f"{BASE}/application/page", json={"pageNum": 1, "pageSize": 5})
recs = r.json()["data"]["records"]
aid = recs[0]["id"]
print(f"申请ID={aid}, 申请号={recs[0]['applicationNo']}")

r = requests.get(f"{BASE}/collaboration/tasks/application/{aid}")
print("返回完整结构:")
print(json.dumps(r.json(), ensure_ascii=False, indent=2))
