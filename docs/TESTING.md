# MUBS 测试文档 (Testing Guide)

## 测试文件

```
src/test/kotlin/com/mubs/
├── security/
│   └── JwtTokenProviderTest.kt        # JWT 生成/验证/过期测试
└── service/
    ├── DispatchServiceTest.kt         # 自动派遣逻辑测试
    ├── TicketStatusTransitionTest.kt  # 状态机转换测试
    └── WebhookVerificationServiceTest.kt  # HMAC 签名验证测试
```

## 运行测试

```bash
cd D:\mubs\backend

# 运行全部测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "com.mubs.service.DispatchServiceTest"

# 查看测试报告
# build/reports/tests/test/index.html
```

---

## 测试覆盖范围

### JwtTokenProviderTest (3 个用例)

| 用例 | 说明 |
|------|------|
| `should generate and validate token` | 生成 Token 后能正常验证, 提取 username |
| `should reject invalid token` | 拒绝格式错误的 Token |
| `should reject expired token` | 拒绝过期 Token |

### DispatchServiceTest (4 个用例)

| 用例 | 说明 |
|------|------|
| `should dispatch smoke_flame in east_district to fire_team` | 精确区域匹配 (优先级 10) |
| `should dispatch smoke_flame in unknown area using wildcard` | 通配符 `*` 回退匹配 |
| `should dispatch parking_violation to traffic_team` | 不同事件类型匹配 |
| `should leave ticket as PENDING when no rules match` | 无匹配规则时保持 PENDING |

### TicketStatusTransitionTest (9 个用例)

| 用例 | 说明 |
|------|------|
| `PENDING → DISPATCHED` | 合法: 新工单派遣 |
| `PENDING → CLOSED` | 合法: 直接关闭 |
| `PENDING → RESOLVED` | 非法: 不能跳过流程 |
| `DISPATCHED → ACCEPTED / RETURNED` | 合法: 接受或退回 |
| `IN_PROGRESS → RESOLVED / RETURNED` | 合法: 完成或退回 |
| `RESOLVED → CLOSED` | 合法: 确认关闭 |
| `CLOSED → 任何状态` | 非法: 终态不可变 |
| `RETURNED → DISPATCHED / CLOSED` | 合法: 重新派遣或关闭 |
| `full happy path` | PENDING→DISPATCHED→ACCEPTED→IN_PROGRESS→RESOLVED→CLOSED |

### WebhookVerificationServiceTest (4 个用例)

| 用例 | 说明 |
|------|------|
| `should verify valid signature` | 正确 HMAC-SHA256 签名通过 |
| `should reject invalid signature` | 错误签名被拒 |
| `should reject null signature` | null 签名被拒 |
| `should reject empty signature` | 空字符串签名被拒 |

---

## 手动 API 测试

### 前置条件

```bash
# 启动 MongoDB
cd D:\mubs && docker compose up -d mongodb

# 启动后端
cd D:\mubs\backend && ./gradlew bootRun
```

### 1. 健康检查

```bash
curl http://localhost:8090/api/health
```

期望: `{"status":"ok","mongodb":"connected"}`

### 2. 登录获取 JWT

```bash
# Admin 登录
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# 保存 Token
TOKEN="<返回的 token 值>"
```

期望: `{"token":"eyJhbG...","username":"admin","role":"ADMIN"}`

### 3. 发送 Webhook (模拟 HVAS 推送)

```bash
# 构造 payload
PAYLOAD='{"event_id":"test-001","event_type":"smoke_flame","camera_id":"cam-01","timestamp":1714000000.0,"created_at":"2026-04-25T10:00:00Z","confidence":0.85,"description":"Smoke detected","area_code":"east_district","group":"fire_team"}'

# 计算 HMAC 签名
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "hvas-mubs-shared-secret" | awk '{print $2}')

# 发送 webhook
curl -X POST http://localhost:8090/api/v1/hvas/webhook \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: $SIGNATURE" \
  -d "$PAYLOAD"
```

期望: `201 {"status":"created","ticket_id":"...","assigned_team":"fire_team"}`

### 4. 重复 Webhook (去重测试)

```bash
# 再次发送相同 event_id
curl -X POST http://localhost:8090/api/v1/hvas/webhook \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: $SIGNATURE" \
  -d "$PAYLOAD"
```

期望: `200 {"status":"duplicate","event_id":"test-001"}`

### 5. 查看工单列表

```bash
curl http://localhost:8090/api/tickets \
  -H "Authorization: Bearer $TOKEN"
```

期望: 分页工单列表, 包含刚创建的工单, 状态为 `DISPATCHED`

### 6. 查看单个工单

```bash
TICKET_ID="<上一步返回的 ticket_id>"
curl http://localhost:8090/api/tickets/$TICKET_ID \
  -H "Authorization: Bearer $TOKEN"
```

期望: 工单详情, 含 timeline 数组 (CREATED + AUTO_DISPATCHED 记录)

### 7. 更新工单状态

```bash
# DISPATCHED → ACCEPTED
curl -X PATCH http://localhost:8090/api/tickets/$TICKET_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED", "note": "已接单"}'

# ACCEPTED → IN_PROGRESS
curl -X PATCH http://localhost:8090/api/tickets/$TICKET_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS", "note": "已到达现场"}'

# IN_PROGRESS → RESOLVED
curl -X PATCH http://localhost:8090/api/tickets/$TICKET_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "RESOLVED", "note": "火已扑灭"}'

# RESOLVED → CLOSED
curl -X PATCH http://localhost:8090/api/tickets/$TICKET_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "CLOSED", "note": "确认关闭"}'
```

每次更新后查看工单详情, 验证 timeline 新增对应记录。

### 8. 查看统计

```bash
curl http://localhost:8090/api/tickets/stats \
  -H "Authorization: Bearer $TOKEN"
```

期望:
```json
{
  "total": 1,
  "byStatus": {"CLOSED": 1},
  "byEventType": {"smoke_flame": 1},
  "byTeam": {"fire_team": 1},
  "avgResolutionMinutes": ...
}
```

### 9. 派遣规则管理 (需 ADMIN)

```bash
# 查看所有规则
curl http://localhost:8090/api/dispatch-rules \
  -H "Authorization: Bearer $TOKEN"

# 创建新规则
curl -X POST http://localhost:8090/api/dispatch-rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"eventType": "crowd_gathering", "areaCode": "*", "targetTeam": "security_team", "priority": 1}'

# 删除规则
RULE_ID="<规则 ID>"
curl -X DELETE http://localhost:8090/api/dispatch-rules/$RULE_ID \
  -H "Authorization: Bearer $TOKEN"
```

### 10. 非法状态转换测试

```bash
# 创建新 Webhook 事件
PAYLOAD2='{"event_id":"test-002","event_type":"parking_violation","camera_id":"cam-02","timestamp":1714000001.0,"created_at":"2026-04-25T10:00:01Z","confidence":0.9,"description":"Parking violation"}'
SIG2=$(echo -n "$PAYLOAD2" | openssl dgst -sha256 -hmac "hvas-mubs-shared-secret" | awk '{print $2}')
curl -X POST http://localhost:8090/api/v1/hvas/webhook \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: $SIG2" \
  -d "$PAYLOAD2"

# 尝试非法转换: DISPATCHED → RESOLVED (应失败)
curl -X PATCH http://localhost:8090/api/tickets/$NEW_TICKET_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "RESOLVED"}'
```

期望: 500 错误, "Invalid transition from DISPATCHED to RESOLVED"

### 11. 错误签名测试

```bash
curl -X POST http://localhost:8090/api/v1/hvas/webhook \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: invalid_signature" \
  -d '{"event_id":"test-bad","event_type":"smoke_flame","camera_id":"cam","timestamp":0,"created_at":"","confidence":0}'
```

期望: `401 {"error":"Invalid signature"}`

---

## 端到端集成测试 (HVAS → MUBS)

### 前置条件
- HVAS 后端运行在 `localhost:5000`
- MUBS 后端运行在 `localhost:8090`
- HVAS 已注册 MUBS 的 Webhook URL

### 步骤

```bash
# 1. 在 HVAS 注册 MUBS Webhook
curl -X POST http://localhost:5000/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8090/api/v1/hvas/webhook"}'

# 2. 确保两端 WEBHOOK_SECRET 一致
# HVAS: WEBHOOK_SECRET=hvas-mubs-shared-secret
# MUBS: HVAS_WEBHOOK_SECRET=hvas-mubs-shared-secret

# 3. 在 HVAS 创建模拟事件 (需 DEMO_MODE=true)
curl -X POST http://localhost:5000/api/events/mock \
  -H "Content-Type: application/json" \
  -d '{"event_type": "smoke_flame", "camera_id": "demo_cam", "area_code": "east_district"}'

# 4. 在 MUBS 检查工单是否自动创建
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl http://localhost:8090/api/tickets?size=1 \
  -H "Authorization: Bearer $TOKEN"
```

期望: MUBS 工单列表中出现 HVAS 推送的事件, 状态为 `DISPATCHED`, 团队为 `fire_team`。

---

## Python 签名验证脚本

便于调试 Webhook 签名问题:

```python
import hmac
import hashlib
import json
import requests

SECRET = "hvas-mubs-shared-secret"
MUBS_URL = "http://localhost:8090/api/v1/hvas/webhook"

payload = {
    "event_id": "py-test-001",
    "event_type": "smoke_flame",
    "camera_id": "cam-01",
    "timestamp": 1714000000.0,
    "created_at": "2026-04-25T10:00:00Z",
    "confidence": 0.85,
    "description": "Test from Python",
    "area_code": "east_district"
}

body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
# 注意: HVAS 使用 json.dumps 默认分隔符, 需要与 HVAS 端一致
body_default = json.dumps(payload).encode("utf-8")
sig = hmac.new(SECRET.encode("utf-8"), body_default, hashlib.sha256).hexdigest()

resp = requests.post(MUBS_URL, data=body_default, headers={
    "Content-Type": "application/json",
    "X-HVAS-Signature": sig
})

print(f"Status: {resp.status_code}")
print(f"Response: {resp.json()}")
```
