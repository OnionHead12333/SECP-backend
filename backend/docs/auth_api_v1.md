# 智慧养老平台 - 注册登录接口文档 (V1)

## 1. 认证接口说明

遵循首版注册绑定流程设计，系统采用统一的认证路径。

### 1.1 用户登录
**地址**: `POST /api/v1/auth/login`

**请求参数**:
```json
{
  "username": "13800138001",
  "password": "密码"
}
```

**成功响应**:
```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "jwt-token-xxx",
    "userId": 1001,
    "role": "elder",
    "name": "姓名",
    "phone": "138xxx",
    "claimed": true,
    "familyCount": 1
  }
}
```

### 1.2 用户注册
**地址**: `POST /api/v1/auth/register`

**请求参数**:
```json
{
  "username": "账号/手机号",
  "password": "密码",
  "role": "elder",
  "phone": "手机号",
  "name": "真实姓名",
  "nickname": "昵称(可选)"
}
```

**响应**:
- `code: 0`: 成功
- 其他 code: 失败（如 1101 用户名已存在）

---

## 2. 数据库核心模型

### 2.1 账号表 (`users`)
- 职责：解决“谁能登录系统”。
- 关键字段：`role` (elder/child), `password_hash`, `phone`。

### 2.2 老人主体表 (`elder_profiles`)
- 职责：唯一老人主体，记录认领状态。
- 关键字段：`claimed_user_id`, `status` (unclaimed/claimed)。

### 2.3 家庭关系表 (`family_bindings`)
- 职责：维护子女与老人绑定关系。
- 关键字段：`status` (pending/active/rejected), `is_primary`。
