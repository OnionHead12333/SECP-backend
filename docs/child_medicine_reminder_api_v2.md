# 子女端接口文档：吃药提醒（V2 - 修订版）

本文档描述子女端吃药提醒接口的完整设计。

---

## 通用

- **前缀**：`/v1/child`
- **鉴权**：需子女登录态（根据 token/session 确定请求者身份）
- **请求 JSON 字段名**：**camelCase**（如下所列）
- **统一响应**：与 `ApiResponse` 一致，`code === 0` 为成功；列表类接口要求 **`data` 为 JSON 数组**

---

## 一、吃药提醒

### 1. 列表

**GET** `/v1/child/medicine-reminders`

| Query 参数 | 类型 | 说明 |
|------------|------|------|
| elderProfileId | int | 必填；当前所选绑定老人档案 ID |

**成功时 `data`**：对象数组；若 `data` 不是数组，前端会解析为空列表。

**列表项字段**（前端会解析；支持 camelCase 或 snake_case）：

| 字段 | 说明 |
|------|------|
| id | int |
| elderProfileId 或 elder_profile_id | int |
| title | string |
| medicineName 或 medicine_name | string |
| dosage | string，可选 |
| frequencyRule 或 frequency_rule | string，可选 |
| repeatRule 或 repeat_rule | string，可选 |
| enabled 或 enabled | boolean，是否启用 |
| remindTime 或 remind_time | ISO8601 字符串，后端已转换为本地时间，前端直接解析展示 |
| sourceType 或 source_type | string，来源：`child_remote`（子女远程）/ `elder_manual`（老人手动） |
| status 或 status | string，状态：`pending`（待执行）/ `completed`（已完成）/ `cancelled`（已取消）/ `timeout`（已过期） |
| createdBy 或 created_by | string，创建人角色：`child`（子女）/ `elder`（老人自己） |

---

### 2. 新建

**POST** `/v1/child/medicine-reminders`

**Body**（每条药品一条请求；多药品会连续 POST 多次）：

| 字段 | 类型 | 说明 |
|------|------|------|
| elderProfileId | int | **必填**；老人档案 ID |
| title | string | **必填**；与药品名相同 |
| medicineName | string | **必填**；药品名称 |
| dosage | string 或 null | 可空；剂量（如：1片/5ml/2粒） |
| frequencyRule | string | 可空；服用频率（如：`daily`/`twice_daily`/`none`） |
| repeatRule | string | 可空；重复规则（如：`daily`/`weekly`/`none`） |
| remindTime | string | **必填**；本地时间的 ISO8601 格式（如 `2026-05-09T08:30:00`），**后端会自动转换为 UTC 存储** |
| enabled | boolean | **必填**；初始启用状态（推荐默认 `true`） |
| relatedEventId | int 或 null | 可空；关联的医疗事件 ID（暂时传 `null` 即可） |

**说明**：
- `sourceType`、`status`、`createdBy` 由后端根据请求者身份**自动设置**，前端无需传递（或传了会被忽略）：
  - `sourceType` → 自动设为 `'child_remote'`（子女远程创建）
  - `status` → 自动设为 `'pending'`（待执行）
  - `createdBy` → 自动设为 `'child'`（由子女创建）

**示例请求**：
```json
{
  "elderProfileId": 123,
  "title": "阿司匹林",
  "medicineName": "阿司匹林",
  "dosage": "1片",
  "frequencyRule": "twice_daily",
  "repeatRule": "daily",
  "remindTime": "2026-05-09T08:30:00",
  "enabled": true,
  "relatedEventId": null
}
```

---

### 3. 修改

**PUT** `/v1/child/medicine-reminders/{id}`

**Body**：

| 字段 | 类型 | 说明 |
|------|------|------|
| title | string | **必填**；与 medicineName 一致 |
| medicineName | string | **必填**；药品名称 |
| dosage | string 或 null | 可空；剂量 |
| frequencyRule | string | 可空；服用频率 |
| repeatRule | string | 可空；重复规则 |
| remindTime | string | **必填**；本地时间 ISO8601 格式，**后端自动转换为 UTC 存储** |
| enabled | boolean | **必填**；是否启用提醒 |
| relatedEventId | int 或 null | 可空；关联的医疗事件 ID |

**说明**：
- 只有这些字段可修改；其他字段（如 `sourceType`、`status`、`createdBy`）由后端维护，不接受前端修改
- `status` 字段由后端自动管理（不提供修改接口）

**示例请求**：
```json
{
  "title": "阿司匹林",
  "medicineName": "阿司匹林",
  "dosage": "1片",
  "frequencyRule": "once_daily",
  "repeatRule": "daily",
  "remindTime": "2026-05-09T09:00:00",
  "enabled": false,
  "relatedEventId": null
}
```

---

### 4. 删除

**DELETE** `/v1/child/medicine-reminders/{id}`

无 Body。

**响应**：成功后前端会再次 **GET 列表** 刷新数据。

---

## 二、时间处理说明

### remindTime 时区规约

为避免跨时区混乱，采用以下方案：

1. **前端发送**：本地时间的 ISO8601 格式（如 `2026-05-09T08:30:00`，不含时区信息）
2. **后端处理**：
   - 接收时，视为**本地时间**（由应用部署时区决定，通常为 `Asia/Shanghai`）
   - 自动转换为 **UTC 时间**后存储到数据库
   - 查询返回给前端时，再转换回**本地时间**格式（同样不含时区信息）
3. **前端展示**：直接按本地时间展示，无需额外转换

### 示例

假设服务器在 `Asia/Shanghai`（UTC+8）：

```
前端发送：  "2026-05-09T08:30:00"
后端存储：  "2026-05-09T00:30:00" (UTC)
前端收到：  "2026-05-09T08:30:00"
前端展示：  2026-05-09 08:30
```

---

## 三、业务规则

1. **新建时状态**：`status` 自动为 `pending`（待执行）
2. **启用/禁用**：通过 PUT 接口修改 `enabled` 字段，前端可随时启用或禁用提醒
3. **删除提醒**：调用 DELETE，彻底删除记录
4. **来源追踪**：`sourceType` 和 `createdBy` 用于区分数据来源，便于后续统计和审计
5. **医疗事件关联**：`relatedEventId` 预留，暂时传 `null`；后续可扩展用于与医疗事件关联

---

## 四、后端决策字段（前端勿传）

以下字段由后端根据请求上下文**自动设置**，前端传递的值会被忽略：

| 字段 | 后端自动值 | 来源 |
|------|-----------|------|
| sourceType | `'child_remote'` | 固定值：表示子女远程创建 |
| status | `'pending'` | 固定值：新建时的初始状态 |
| createdBy | `'child'` | 自动：根据请求者身份确定 |
| elderProfileId | 从请求体获取 | 前端必传，后端验证 |

---

## 五、错误响应

**示例错误响应**：

```json
{
  "code": -1,
  "message": "老人档案不存在或无权限访问",
  "data": null
}
```

常见错误：
- `elderProfileId` 不存在
- 老人档案无子女访问权限
- `remindTime` 格式错误
- 必填字段缺失

---

## 六、备注

- 列表接口、新建接口、修改接口均支持 **camelCase 和 snake_case** 混用（前后端自行协商）
- 前端在修改后应重新 GET 列表，确保 UI 与服务端数据同步
- 删除后，对应的 `reminder_execution_logs`（执行记录）由后端异步清理或软删除处理（具体由后端决定）

