# MUBS 开发文档 (Development Guide)

## 项目概述

**MUBS** (Municipal Urban Business System) — 城市管理业务系统，接收 HVAS 视频分析事件，创建工单，自动派遣到处置团队，提供管理仪表盘和移动端 H5 应用的 REST API。

- **后端**: Kotlin Spring Boot 3.x, 端口 8090
- **数据库**: MongoDB (端口 27018, 与 HVAS 27017 分开)
- **构建工具**: Gradle 9.4.1 Kotlin DSL
- **JDK**: 21+ (开发环境使用 Zulu 25)
- **项目路径**: `D:\mubs` (monorepo), 后端在 `D:\mubs\backend`

---

## 项目结构

```
D:\mubs\
├── docker-compose.yml                 # MongoDB + Backend 容器编排
├── docs/
│   ├── DEVELOPMENT.md                 # 本文档
│   └── TESTING.md                     # 测试文档
└── backend\
    ├── build.gradle.kts               # Gradle 构建脚本
    ├── settings.gradle.kts            # 项目名称配置
    ├── gradlew / gradlew.bat          # Gradle Wrapper
    ├── Dockerfile                     # 多阶段构建 (JDK 21 → JRE Alpine)
    ├── .gitignore
    └── src/
        ├── main/kotlin/com/mubs/
        │   ├── MubsApplication.kt     # 入口 (@SpringBootApplication + @EnableScheduling)
        │   ├── config/
        │   │   ├── SecurityConfig.kt  # Spring Security 过滤链
        │   │   ├── WebSocketConfig.kt # STOMP/SockJS 配置
        │   │   ├── WebConfig.kt       # CORS 配置
        │   │   └── WebhookBodyCacheFilter.kt  # Webhook Body 缓存过滤器
        │   ├── model/
        │   │   ├── Ticket.kt          # 工单 (含 TimelineEntry)
        │   │   ├── User.kt            # 用户
        │   │   ├── DispatchRule.kt    # 派遣规则
        │   │   ├── NotificationLog.kt # 通知日志
        │   │   └── enums/
        │   │       ├── TicketStatus.kt    # 状态枚举 + 状态机
        │   │       ├── UserRole.kt        # ADMIN/DISPATCHER/FIELDWORKER
        │   │       └── NotificationChannel.kt  # EMAIL/WEBSOCKET/SMS
        │   ├── repository/            # Spring Data MongoDB 接口
        │   │   ├── TicketRepository.kt
        │   │   ├── UserRepository.kt
        │   │   ├── DispatchRuleRepository.kt
        │   │   └── NotificationLogRepository.kt
        │   ├── service/
        │   │   ├── TicketService.kt           # 工单 CRUD + 统计
        │   │   ├── DispatchService.kt         # 自动派遣 + 超时回收
        │   │   ├── AuthService.kt             # 登录认证
        │   │   ├── NotificationService.kt     # WebSocket + 邮件通知
        │   │   ├── WebhookVerificationService.kt  # HMAC-SHA256 签名验证
        │   │   └── HvasPollingService.kt      # HVAS 轮询备用方案
        │   ├── controller/
        │   │   ├── AuthController.kt          # POST /api/auth/login
        │   │   ├── TicketController.kt        # GET/PATCH /api/tickets
        │   │   ├── HvasWebhookController.kt   # POST /api/v1/hvas/webhook
        │   │   ├── DispatchRuleController.kt  # CRUD /api/dispatch-rules
        │   │   └── HealthController.kt        # GET /api/health
        │   ├── security/
        │   │   ├── JwtTokenProvider.kt        # JWT 生成/验证 (JJWT)
        │   │   ├── JwtAuthenticationFilter.kt # Bearer Token 过滤器
        │   │   └── UserDetailsServiceImpl.kt  # Spring Security UserDetails
        │   ├── dto/
        │   │   ├── AuthDtos.kt                # LoginRequest/LoginResponse
        │   │   ├── TicketDtos.kt              # TicketStatusUpdateRequest, TicketFilterParams, TicketStatsResponse
        │   │   ├── HvasWebhookPayload.kt      # HVAS Webhook 载荷
        │   │   └── DispatchRuleDto.kt         # 派遣规则 DTO
        │   └── seed/
        │       └── DataSeeder.kt              # 初始化种子数据
        └── main/resources/
            └── application.yml                # 应用配置
```

---

## 快速启动

### 1. 启动 MongoDB

```bash
cd D:\mubs
docker compose up -d mongodb    # MongoDB 启动在端口 27018
```

### 2. 运行后端

```bash
cd D:\mubs\backend
./gradlew bootRun
```

启动后自动:
- 连接 MongoDB (localhost:27018)
- 种子数据写入 (3 个用户 + 4 条派遣规则)
- 启动派遣超时定时任务

### 3. 验证

```bash
# 健康检查
curl http://localhost:8090/api/health

# 登录获取 JWT
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

---

## API 接口

### 认证 (公开)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录获取 JWT |

**请求**:
```json
{"username": "admin", "password": "admin123"}
```

**响应**:
```json
{"token": "eyJhbG...", "username": "admin", "role": "ADMIN"}
```

后续请求携带 Header: `Authorization: Bearer <token>`

### 健康检查 (公开)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 系统状态 |

```json
{"status": "ok", "mongodb": "connected"}
```

### HVAS Webhook (公开, HMAC 验证)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/hvas/webhook` | 接收 HVAS 事件 |

**请求 Header**: `X-HVAS-Signature: <HMAC-SHA256 hex digest>`

**请求 Body**: HVAS 标准事件 JSON (见 HVAS 开发文档)

**处理流程**:
1. HMAC 签名验证
2. 去重检查 (event_id)
3. 创建工单 (Ticket)
4. 自动派遣 (匹配 DispatchRule)
5. WebSocket + 邮件通知

**响应 (201)**:
```json
{
  "status": "created",
  "ticket_id": "663f...",
  "assigned_team": "fire_team"
}
```

### 工单管理 (需认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tickets` | 工单列表 (分页/过滤) |
| GET | `/api/tickets/{id}` | 工单详情 |
| PATCH | `/api/tickets/{id}/status` | 更新工单状态 |
| GET | `/api/tickets/stats` | 聚合统计 |

**查询参数 (GET /api/tickets)**:
- `status` — PENDING / DISPATCHED / ACCEPTED / IN_PROGRESS / RESOLVED / CLOSED / RETURNED
- `eventType` — smoke_flame / parking_violation / common_space_utilization
- `assignedTeam` — fire_team / traffic_team / urban_mgmt_team
- `page` (默认 0) / `size` (默认 20)

**更新状态 (PATCH /api/tickets/{id}/status)**:
```json
{"status": "RESOLVED", "note": "已到现场处理完毕"}
```

**统计 (GET /api/tickets/stats)**:
```json
{
  "total": 42,
  "byStatus": {"PENDING": 5, "DISPATCHED": 10, "RESOLVED": 27},
  "byEventType": {"smoke_flame": 15, "parking_violation": 20, "common_space_utilization": 7},
  "byTeam": {"fire_team": 15, "traffic_team": 20, "urban_mgmt_team": 7},
  "avgResolutionMinutes": 45.3
}
```

### 派遣规则管理 (需 ADMIN 角色)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dispatch-rules` | 所有规则 |
| GET | `/api/dispatch-rules/{id}` | 规则详情 |
| POST | `/api/dispatch-rules` | 创建规则 |
| PUT | `/api/dispatch-rules/{id}` | 更新规则 |
| DELETE | `/api/dispatch-rules/{id}` | 删除规则 |

**规则模型**:
```json
{
  "eventType": "smoke_flame",
  "areaCode": "east_district",
  "targetTeam": "fire_team",
  "priority": 10,
  "enabled": true
}
```

匹配逻辑: 按 `eventType` 查找规则 → 按 `priority` 降序 → 优先匹配精确 `areaCode`, 其次匹配通配符 `*`。

---

## 工单状态机

```
PENDING ──→ DISPATCHED ──→ ACCEPTED ──→ IN_PROGRESS ──→ RESOLVED ──→ CLOSED
  │              │              │              │
  └──→ CLOSED    └──→ RETURNED ←┘              └──→ RETURNED
                       │                              │
                       └──→ DISPATCHED ←──────────────┘
                       └──→ CLOSED
```

**合法转换**:
| 当前状态 | 可转为 |
|----------|--------|
| PENDING | DISPATCHED, CLOSED |
| DISPATCHED | ACCEPTED, RETURNED |
| ACCEPTED | IN_PROGRESS, RETURNED |
| IN_PROGRESS | RESOLVED, RETURNED |
| RESOLVED | CLOSED |
| RETURNED | DISPATCHED, CLOSED |
| CLOSED | (终态) |

---

## WebSocket 实时推送

**STOMP 端点**: `ws://localhost:8090/ws` (支持 SockJS 回退)

**订阅 Topic**:

| Topic | 说明 |
|-------|------|
| `/topic/tickets/all` | 全局工单变更广播 |
| `/topic/tickets/{team}` | 团队工单广播 (如 `/topic/tickets/fire_team`) |

**前端订阅示例**:
```javascript
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

const socket = new SockJS('http://localhost:8090/ws')
const client = Stomp.over(socket)

client.connect({}, () => {
  // 订阅全部工单
  client.subscribe('/topic/tickets/all', (msg) => {
    const ticket = JSON.parse(msg.body)
    console.log('工单更新:', ticket)
  })

  // 订阅特定团队
  client.subscribe('/topic/tickets/fire_team', (msg) => {
    const ticket = JSON.parse(msg.body)
    console.log('消防队工单:', ticket)
  })
})
```

---

## 种子数据

### 用户

| 用户名 | 密码 | 角色 | 团队 |
|--------|------|------|------|
| `admin` | `admin123` | ADMIN | - |
| `dispatcher_wang` | `dispatch123` | DISPATCHER | - |
| `worker_zhang` | `worker123` | FIELDWORKER | fire_team |

### 派遣规则

| 事件类型 | 区域 | 目标团队 | 优先级 |
|----------|------|----------|--------|
| smoke_flame | east_district | fire_team | 10 |
| smoke_flame | * | fire_team | 1 |
| parking_violation | * | traffic_team | 1 |
| common_space_utilization | * | urban_mgmt_team | 1 |

---

## 配置参考

### application.yml 关键配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| `spring.data.mongodb.uri` | `MONGODB_URI` | `mongodb://localhost:27018/mubs` | MongoDB 连接串 |
| `server.port` | `SERVER_PORT` | `8090` | 服务端口 |
| `mubs.jwt.secret` | `JWT_SECRET` | (内置默认) | JWT 签名密钥 (生产必改) |
| `mubs.jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` (24h) | JWT 过期时间 |
| `mubs.hvas.webhook-secret` | `HVAS_WEBHOOK_SECRET` | `hvas-mubs-shared-secret` | HMAC 共享密钥 |
| `mubs.hvas.polling.enabled` | `HVAS_POLLING_ENABLED` | `false` | HVAS 轮询开关 |
| `mubs.hvas.polling.interval-ms` | `HVAS_POLLING_INTERVAL_MS` | `30000` | 轮询间隔 (毫秒) |
| `mubs.dispatch.timeout-minutes` | `DISPATCH_TIMEOUT_MINUTES` | `30` | 派遣超时 (分钟) |
| `spring.mail.enabled` | `MAIL_ENABLED` | `false` | 邮件通知开关 |

---

## 安全架构

### 认证流程
1. 客户端 POST `/api/auth/login` 提交凭据
2. AuthService 通过 Spring Security AuthenticationManager 校验
3. JwtTokenProvider 生成 JWT (含 username 和 role)
4. 客户端后续请求携带 `Authorization: Bearer <token>`
5. JwtAuthenticationFilter 拦截并验证 Token
6. SecurityContext 中注入用户身份

### 访问控制
- **公开端点**: `/api/auth/**`, `/api/health`, `/api/v1/hvas/webhook`, `/ws/**`
- **需认证**: 所有其他端点
- **需 ADMIN**: `/api/dispatch-rules/**`

### Webhook 安全
Webhook 不使用 JWT, 使用 HMAC-SHA256 签名验证 (服务器到服务器通信, HVAS 无 MUBS 用户身份):
1. `WebhookBodyCacheFilter` 缓存请求体 (ContentCachingRequestWrapper)
2. `WebhookVerificationService` 用共享密钥计算 HMAC, 对比 `X-HVAS-Signature` 请求头

---

## MongoDB 集合

| 集合 | 索引 | 说明 |
|------|------|------|
| `tickets` | hvasEventId, status, assignedTeam | 工单 |
| `users` | username (唯一) | 用户 |
| `dispatch_rules` | - | 派遣规则 |
| `notification_logs` | - | 通知日志 |

---

## 构建与部署

### 本地构建

```bash
cd D:\mubs\backend
./gradlew build          # 编译 + 测试
./gradlew bootJar        # 仅打包
./gradlew bootRun        # 直接运行
```

### Docker 构建

```bash
cd D:\mubs
docker compose up -d                # 启动全部 (MongoDB + Backend)
docker compose up -d mongodb        # 仅启动 MongoDB
docker compose build backend        # 重新构建后端镜像
```

### 生产部署注意

1. 修改 `JWT_SECRET` (至少 32 字符)
2. 修改 `HVAS_WEBHOOK_SECRET` (与 HVAS 端一致)
3. 配置邮件服务 (`MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_ENABLED=true`)
4. MongoDB 添加认证
