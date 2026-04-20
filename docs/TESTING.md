# MUBS 测试文档 (Testing Guide)

## 自动化测试

```bash
cd D:\mubs\backend
./gradlew test                    # 全部测试
./gradlew test --tests "com.mubs.service.DispatchServiceTest"  # 单个类
```

### 测试覆盖

| 测试类 | 用例数 | 说明 |
|--------|--------|------|
| JwtTokenProviderTest | 3 | JWT 生成/验证/过期 |
| DispatchServiceTest | 4 | 自动派遣 (精确匹配/通配符/无规则) |
| TicketStatusTransitionTest | 9 | 状态机合法/非法转换 + 完整路径 |
| WebhookVerificationServiceTest | 4 | HMAC 签名验证/拒绝 |

---

## 集成测试脚本

### MUBS 全流程测试

```bash
bash D:\mubs\tests\integration_test.sh
```

覆盖: 健康检查 → 登录 → 种子数据 → 模拟事件 → 自动派遣 → 完整生命周期 (accept→in_progress→resolve→close) → 时间线 → 重派 → 派遣规则 → 外勤登录 → 统计

### Webhook 签名测试

```bash
bash D:\mubs\tests\webhook_integration_test.sh
```

覆盖: HMAC 签名构造 → 工单创建 → 自动派遣验证 → 重复事件拒绝 → 无效签名 401 → 缺失签名 401

---

## 端到端联调 (HVAS → MUBS)

### 前置条件
- HVAS 后端运行 (默认端口 5000)
- MUBS 后端运行 (端口 8090)
- 两端 `WEBHOOK_SECRET` 一致 (`hvas-mubs-shared-secret`)

### 步骤

```bash
# 1. 注册 Webhook
curl -X POST http://localhost:5000/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8090/api/v1/hvas/webhook"}'

# 2. 触发检测 (放视频到 uploads/smoke_flame/ 或使用 RTSP 流)

# 3. 验证 MUBS 工单自动创建
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl http://localhost:8090/api/tickets?size=1 -H "Authorization: Bearer $TOKEN"
```

---

## 手动 API 测试

### 登录

```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### 模拟 Webhook (带 HMAC 签名)

```bash
PAYLOAD='{"event_id":"test-001","event_type":"smoke_flame","camera_id":"cam-01","timestamp":1714000000.0,"confidence":0.85,"description":"Smoke detected","area_code":"east_district"}'
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "hvas-mubs-shared-secret" | awk '{print $NF}')

curl -X POST http://localhost:8090/api/v1/hvas/webhook \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: $SIGNATURE" \
  -d "$PAYLOAD"
```

### 工单状态流转

```bash
# DISPATCHED → ACCEPTED → IN_PROGRESS → RESOLVED → CLOSED
curl -X PATCH http://localhost:8090/api/tickets/$ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED", "note": "已接单"}'
```

### 通知测试

邮件和短信通知需要配置阿里云凭据后才能测试:

```bash
# 启动时设置环境变量
set EMAIL_ENABLED=true
set ALIYUN_MAIL_USER=xxx@yourdomain.com
set ALIYUN_MAIL_PASSWORD=xxx
set SMS_ENABLED=true
set ALIYUN_SMS_ACCESS_KEY_ID=xxx
set ALIYUN_SMS_ACCESS_KEY_SECRET=xxx
```

触发模拟事件后检查:
- 团队负责人邮箱收到工单通知邮件
- 外勤人员手机收到派遣短信
