# MUBS 开发文档 (Development Guide)

## 项目概述

**MUBS** (Municipal Urban Business System) — 城市管理业务系统，接收 HVAS 视频分析事件，创建工单，自动派遣到处置团队，提供管理仪表盘和移动端 APP。

### 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        HVAS (视频分析)                           │
│  YOLO 检测 → Qwen-VL 验证 → Webhook POST → MUBS               │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HMAC-SHA256 签名
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MUBS 后端 (Spring Boot)                  │
│  Webhook 接收 → 建单 → 自动派遣 → 通知 (WebSocket/邮件/短信)    │
│  REST API → JWT 认证 → 工单 CRUD → 状态机 → HVAS 状态回传       │
└──────┬──────────────────┬──────────────────┬────────────────────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌────────────┐   ┌──────────────┐   ┌──────────────────┐
│ Web 管理端  │   │  移动端 APP   │   │  阿里云通知服务   │
│ Vue 3 +    │   │ Compose      │   │  DirectMail 邮件  │
│ Element    │   │ Multiplatform│   │  SMS 短信         │
│ Plus       │   │ (Android/iOS)│   │                  │
└────────────┘   └──────────────┘   └──────────────────┘
```

### 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Kotlin Spring Boot 3.x, 端口 8090 |
| 数据库 | MongoDB (端口 27018) |
| Web 管理端 | Vue 3 + TypeScript + Element Plus + ECharts |
| 移动端 APP | Compose Multiplatform (Android + iOS) |
| 消息通知 | WebSocket (STOMP) + 阿里云 DirectMail + 阿里云 SMS |
| 构建工具 | Gradle 9.4.1 (后端), Vite (Web), Gradle KMP (APP) |
| JDK | 21+ (开发环境 Zulu 25) |

---

## 项目结构

```
D:\mubs\
├── docs/
│   ├── DEVELOPMENT.md                 # 本文档
│   └── TESTING.md                     # 测试文档
├── backend\                           # Spring Boot 后端
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/mubs/
│       ├── config/
│       │   ├── SecurityConfig.kt      # Spring Security
│       │   ├── WebSocketConfig.kt     # STOMP/SockJS
│       │   ├── AsyncConfig.kt         # 异步线程池
│       │   └── WebConfig.kt           # CORS
│       ├── model/                     # MongoDB 文档模型
│       ├── repository/                # Spring Data MongoDB
│       ├── service/
│       │   ├── TicketService.kt       # 工单 CRUD + 统计
│       │   ├── DispatchService.kt     # 自动派遣 + 超时回收
│       │   ├── NotificationService.kt # WebSocket + 邮件 + 短信
│       │   ├── EmailService.kt        # 阿里云 DirectMail
│       │   ├── SmsService.kt          # 阿里云 SMS
│       │   ├── HvasApiClient.kt       # HVAS 状态回传
│       │   └── DemoService.kt         # 演示模式
│       ├── controller/                # REST API
│       ├── security/                  # JWT 认证
│       ├── dto/                       # 数据传输对象
│       └── seed/DataSeeder.kt         # 种子数据
├── frontend\                          # Vue 3 Web 管理端
│   ├── vite.config.ts
│   └── src/
│       ├── shared/                    # 共享 (API/stores/types/utils)
│       └── web/                       # Web Dashboard 页面
│           ├── views/
│           │   ├── Login.vue
│           │   ├── Dashboard.vue      # 统计 + ECharts + 实时刷新
│           │   ├── TicketList.vue      # 过滤表格 + 分页
│           │   ├── TicketDetail.vue    # 详情 + 时间线 + 操作
│           │   └── DispatchRules.vue   # CRUD (ADMIN)
│           ├── layouts/DefaultLayout.vue
│           └── router/index.ts
├── mobile\                            # Compose Multiplatform APP (新)
│   ├── build.gradle.kts               # KMP 构建脚本
│   ├── composeApp/
│   │   ├── src/
│   │   │   ├── commonMain/            # 共享代码
│   │   │   │   ├── data/              # API 客户端 (Ktor)
│   │   │   │   ├── domain/            # 数据模型 + 状态机
│   │   │   │   └── ui/                # Compose UI
│   │   │   │       ├── screens/       # 登录/任务列表/详情/处置
│   │   │   │       ├── components/    # 通用组件
│   │   │   │       └── theme/         # Material 3 主题
│   │   │   ├── androidMain/           # Android 特定 (推送/相机)
│   │   │   └── iosMain/               # iOS 特定 (推送/相机)
│   │   └── build.gradle.kts
│   └── iosApp/                        # iOS Xcode 项目壳
└── tests/
    ├── integration_test.sh            # MUBS 集成测试
    └── webhook_integration_test.sh    # Webhook 签名测试
```

---

## 变更记录 (需求变更)

### 移除: H5 移动端 + 微信小程序
- 删除 `frontend/src/h5/` 目录及相关路由
- 删除 `index-h5.html` 入口
- 删除 `main-h5.ts` 引导文件
- 移除 Vant 4 依赖

### 新增: Compose Multiplatform 移动端 APP
- 技术栈: Compose Multiplatform (Kotlin), 支持 Android + iOS
- HTTP: Ktor Client + JWT 拦截器
- 状态管理: ViewModel + StateFlow
- 导航: Compose Navigation
- 图片: Coil 3 (Multiplatform)
- 推送: Firebase Cloud Messaging (Android) / APNs (iOS)

### 新增: 阿里云邮件 + 短信通知
- 邮件: 阿里云 DirectMail API (SMTP 协议)
- 短信: 阿里云 SMS API (dysmsapi)
- 触发时机: 工单创建/派遣/超时/状态变更
- 通知对象: 团队负责人 (邮件) + 外勤人员 (短信)

---

## 开发计划

### Phase 1: HVAS API 加固 ✅
### Phase 2: MUBS 后端 ✅
### Phase 3: MUBS Web 前端 ✅
### Phase 4: 联调 + 修复 ✅

### Phase 5: 后端通知增强 — 阿里云邮件 + 短信

#### 5.1 阿里云 DirectMail 邮件通知
- 添加 `spring-boot-starter-mail` 依赖
- 新增 `EmailService.kt`:
  - SMTP 配置: `smtpdm.aliyun.com:465` (SSL)
  - 模板: 工单创建通知、派遣通知、超时提醒、状态变更
  - 收件人: 根据 `assignedTeam` 查找团队负责人邮箱
- `application.yml` 新增配置:
  ```yaml
  spring.mail:
    host: smtpdm.aliyun.com
    port: 465
    username: ${ALIYUN_MAIL_USER}
    password: ${ALIYUN_MAIL_PASSWORD}
    properties.mail.smtp.ssl.enable: true
  mubs.notification:
    email.enabled: ${EMAIL_ENABLED:false}
    email.from: ${ALIYUN_MAIL_FROM:noreply@yourdomain.com}
  ```

#### 5.2 阿里云 SMS 短信通知
- 添加阿里云 SDK 依赖: `alibabacloud-dysmsapi20170525`
- 新增 `SmsService.kt`:
  - 签名 + 模板 ID 配置
  - 发送场景: 新工单派遣 (外勤人员手机号)、超时提醒
  - 模板变量: 工单类型、位置、团队
- `application.yml` 新增配置:
  ```yaml
  mubs.notification:
    sms.enabled: ${SMS_ENABLED:false}
    sms.access-key-id: ${ALIYUN_SMS_ACCESS_KEY_ID}
    sms.access-key-secret: ${ALIYUN_SMS_ACCESS_KEY_SECRET}
    sms.sign-name: ${ALIYUN_SMS_SIGN_NAME:MUBS城管}
    sms.template-code.dispatch: ${ALIYUN_SMS_TPL_DISPATCH:SMS_xxx}
    sms.template-code.timeout: ${ALIYUN_SMS_TPL_TIMEOUT:SMS_xxx}
  ```

#### 5.3 User 模型扩展
- `User.kt` 新增字段: `email`, `phone`, `team`
- `DataSeeder.kt` 更新种子用户数据
- `NotificationService.kt` 整合 WebSocket + Email + SMS

### Phase 6: Compose Multiplatform 移动端 APP

#### 6.1 项目脚手架
- 初始化 KMP 项目 (`mobile/`)
- 配置 Gradle: Compose Multiplatform plugin, Ktor, Kotlinx Serialization
- 共享模块: `commonMain` (API/UI/导航)
- 平台模块: `androidMain` + `iosMain`

#### 6.2 共享数据层 (commonMain)
- `data/api/MubsApiClient.kt`: Ktor HttpClient + JWT Bearer 拦截器
- `data/api/AuthApi.kt`: login
- `data/api/TicketApi.kt`: list, getById, updateStatus, uploadPhoto
- `domain/model/`: Ticket, TicketStatus, TimelineEntry, User
- `domain/StatusTransitions.kt`: 状态转换表 (复制后端)

#### 6.3 共享 UI 层 (commonMain)
- `ui/screens/LoginScreen.kt`: Material 3 登录表单
- `ui/screens/TaskListScreen.kt`: LazyColumn + 下拉刷新 + 状态筛选
- `ui/screens/TaskDetailScreen.kt`: 信息卡片 + 时间线 + 证据图片 + 操作按钮
- `ui/screens/HandleTaskScreen.kt`: 相机拍照 + 备注 + 提交 (→ RESOLVED)
- `ui/screens/HistoryScreen.kt`: 已完成工单列表
- `ui/components/`: StatusChip, TimelineView, TicketCard
- `ui/theme/`: Material 3 主题 (亮/暗)

#### 6.4 Android 特定
- `AndroidManifest.xml`: 网络/相机/存储权限
- Firebase Cloud Messaging 推送集成
- CameraX 拍照实现

#### 6.5 iOS 特定
- APNs 推送集成
- UIKit 相机桥接

#### 6.6 APP 功能清单

| 页面 | 功能 |
|------|------|
| 登录 | 用户名/密码 → JWT |
| 任务列表 | 下拉刷新, 按状态筛选, 显示本团队工单 |
| 任务详情 | 基本信息, 证据图片, 时间线, 状态操作按钮 |
| 处置上报 | 拍照 + 备注 → 上传照片 → RESOLVED |
| 历史工单 | RESOLVED/CLOSED 列表 |
| 推送通知 | 新工单派遣时推送到外勤人员手机 |

### Phase 7: Docker Compose 全栈部署
- MongoDB × 2 + HVAS + MUBS 后端 + Nginx (反代 Web 前端 + API)
- 环境变量统一管理 (.env)

### Phase 8: 生产加固
- API 限流 (Spring Rate Limiter)
- 操作审计日志
- 前端错误边界
- Prometheus + Grafana 监控

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

### HVAS Webhook (公开, HMAC 验证)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/hvas/webhook` | 接收 HVAS 事件 |

**Header**: `X-HVAS-Signature: <HMAC-SHA256 hex digest>`

**处理流程**: HMAC 验证 → 去重 → 建单 → 自动派遣 → WebSocket + 邮件 + 短信通知

### 工单管理 (需认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tickets` | 工单列表 (分页/过滤) |
| GET | `/api/tickets/{id}` | 工单详情 |
| PATCH | `/api/tickets/{id}/status` | 更新状态 |
| PATCH | `/api/tickets/{id}/reassign` | 重派 (ADMIN/DISPATCHER) |
| POST | `/api/tickets/{id}/photos` | 上传处置照片 |
| GET | `/api/tickets/stats` | 聚合统计 |

### 派遣规则 (ADMIN)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/api/dispatch-rules` | 列表/创建 |
| PUT/DELETE | `/api/dispatch-rules/{id}` | 更新/删除 |

### 演示模式 (ADMIN)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/demo/simulate-event` | 模拟事件 |

---

## 工单状态机

```
PENDING → DISPATCHED → ACCEPTED → IN_PROGRESS → RESOLVED → CLOSED
  │            │            │            │
  └→ CLOSED    └→ RETURNED ←┘            └→ RETURNED
                    │                          │
                    └→ DISPATCHED ←────────────┘
                    └→ CLOSED
```

---

## 通知策略

| 事件 | WebSocket | 邮件 | 短信 |
|------|-----------|------|------|
| 新工单创建 | ✅ 全局广播 | ✅ 团队负责人 | ❌ |
| 工单派遣 | ✅ 团队广播 | ✅ 团队负责人 | ✅ 外勤人员 |
| 状态变更 | ✅ 全局广播 | ❌ | ❌ |
| 派遣超时 | ✅ 全局广播 | ✅ 管理员 | ✅ 团队负责人 |
| 工单关闭 | ✅ 全局广播 | ❌ | ❌ |

---

## 种子数据

### 用户

| 用户名 | 密码 | 角色 | 团队 | 邮箱 | 手机 |
|--------|------|------|------|------|------|
| admin | admin123 | ADMIN | - | admin@mubs.local | - |
| dispatcher_wang | dispatch123 | DISPATCHER | - | wang@mubs.local | - |
| worker_zhang | worker123 | FIELDWORKER | fire_team | zhang@mubs.local | 13800000001 |
| worker_li | worker123 | FIELDWORKER | traffic_team | li@mubs.local | 13800000002 |
| worker_chen | worker123 | FIELDWORKER | urban_mgmt_team | chen@mubs.local | 13800000003 |

### 派遣规则

| 事件类型 | 区域 | 目标团队 | 优先级 |
|----------|------|----------|--------|
| smoke_flame | east_district | fire_team | 10 |
| smoke_flame | * | fire_team | 1 |
| parking_violation | * | traffic_team | 1 |
| common_space_utilization | * | urban_mgmt_team | 1 |

---
