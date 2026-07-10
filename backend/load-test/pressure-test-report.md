# SECP 智慧养老平台后端本地压力测试报告

## 1. 测试目标

本次测试目标是用 k6 在本地环境验证后端核心接口的基础性能、并发稳定性和业务断言表现，优先覆盖健康检查、登录与绑定老人读取、老人定位上传、喝水提醒、SOS、子女告警列表、医疗档案轻量读取。

本报告不把本地测试结果等同于线上承载能力。它只能说明当前电脑、本地 MySQL、当前配置下的表现。

## 2. 测试环境

| 项目 | 内容 |
|---|---|
| 测试日期 | 2026-06-06 |
| 后端目录 | `D:\BJTU6\SECP\SCEP-backend\backend` |
| 技术栈 | Spring Boot, Java 17, MySQL, JWT, JPA |
| 运行方式 | 本地 `.\mvnw.cmd spring-boot:run` |
| 接口地址 | `http://localhost:8080/api` |
| 数据库 | 本地 MySQL，库名 `elder` |
| 压测工具 | k6 |
| 外部服务 | 不压 OCR、语音、AI 外部服务 |

## 3. 测试场景表

| 场景 | 脚本 | VUs | duration | checks | thresholds |
|---|---|---:|---:|---|---|
| 健康检查 | `health-smoke.js` | 50 | 3m | HTTP 200, code ok | `http_req_failed < 1%`, `P95 < 200ms` |
| 登录 + 绑定老人读取 | `login-bound-elders.js` | 30 | 5m | 登录成功, token, 绑定列表非空 | `http_req_failed < 2%`, `P95 < 800ms` |
| 老人定位上传 | `location-upload.js` | 30 | 5m | 上传成功, code 0, 返回 locationId | `http_req_failed < 2%`, `P95 < 800ms` |
| 喝水提醒创建 + 查询 | `water-reminder-create-query.js` | 10 | 3m | 创建成功, 返回 id, 查询列表非空 | `http_req_failed < 2%`, `P95 < 1000ms`, `checks > 98%` |
| SOS 发起 + 查看 | `sos-create-view.js` | 10 | 3m | 创建 pending, send-now, 老人详情, 子女列表 | `http_req_failed < 2%`, `P95 < 1000ms`, `checks > 98%` |
| 子女告警列表读取 | `child-alerts-list.js` | 20 | 3m | 列表成功, 包含 setup 种子告警 | `http_req_failed < 2%`, `P95 < 800ms`, `checks > 99%` |
| 医疗档案轻量读取 | `medical-archive-read.js` | 15 | 3m | 文件夹列表, 文档列表 | `http_req_failed < 2%`, `P95 < 800ms`, `checks > 99%` |

## 4. 已完成测试结果

| 场景 | 请求数 | RPS | HTTP 错误率 | P95 | checks |
|---|---:|---:|---:|---:|---:|
| 健康检查 | 9000 | 49.88 | 0% | 3.26ms | 100% |
| 登录 + 绑定老人读取 | 15841 | 52.60 | 0% | 234.17ms | 100% |
| 老人定位上传 | 8852 | 29.48 | 0% | 63.96ms | 99.97% |
| 喝水提醒创建 + 查询 | 待运行 | 待记录 | 待记录 | 待记录 | 待记录 |
| SOS 发起 + 查看 | 待运行 | 待记录 | 待记录 | 待记录 | 待记录 |
| 子女告警列表读取 | 待运行 | 待记录 | 待记录 | 待记录 | 待记录 |
| 医疗档案轻量读取 | 待运行 | 待记录 | 待记录 | 待记录 | 待记录 |

## 5. 性能结论

健康检查非常快，50 VU、3 分钟下 P95 约 3.26ms，可作为本地网络与 Spring Boot 基线。

登录 + 绑定老人读取在 30 VU、5 分钟下 HTTP 错误率为 0，P95 约 234.17ms。该场景涉及密码校验、JWT 签发、用户查询和家庭绑定查询，比健康检查慢是正常的。

定位上传在 30 VU、5 分钟下 HTTP 错误率为 0，P95 约 63.96ms，整体响应较快。但出现少量业务断言失败，说明存在非 HTTP 层面的业务返回异常或并发初始化干扰。

新增脚本尚需运行后补齐结果。写入型脚本默认 VU 更小，避免本地数据库产生过多测试数据。

## 6. 发现的问题

定位上传旧脚本让所有 VU 共享同一个新老人账号。后端上传定位时会更新或创建定位守护设置，首次并发可能同时创建设置记录，导致后续查询期望单条记录时出现业务异常。已在 `location-upload.js` 的 setup 中加入一次预热上传，先单线程完成初始化。

当前 Codex 运行环境找不到 `k6` 命令，无法在这里直接执行 `k6 inspect` 或完整压测。但新增脚本已用 `node --check` 做过 JavaScript 语法检查。

## 7. 本地压测局限性

本地压测不代表线上能力。本地通常没有网关、负载均衡、真实网络延迟、线上数据库规格、连接池隔离、日志采集、容器资源限制和真实用户分布。

本地 MySQL 与 Spring Boot 在同一台机器上，会互相争 CPU、内存和磁盘 IO。压测结果可能受电脑当前负载影响。

本次不压 OCR、语音、AI 外部服务，所以报告只覆盖后端普通 API 和本地数据库路径，不覆盖外部 API 延迟、失败率和限流。

写入型脚本会产生少量测试账号、老人档案、提醒、告警和文件夹。默认配置已经控制规模，但多次运行后本地库会积累测试数据，需要在开发库里定期清理。

## 8. 后续记录方式

每次运行后记录以下字段：

| 场景 | VUs | duration | 请求数 | RPS | P95 | HTTP 错误率 | checks | 备注 |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| 示例 | 10 | 3m | 1234 | 6.85 | 120ms | 0% | 100% | 无 |

PowerShell 读取 JSON 示例：

```powershell
$r = Get-Content load-test\reports\water-reminder-create-query.json | ConvertFrom-Json
$r.metrics.http_reqs.count
$r.metrics.http_reqs.rate
$r.metrics.http_req_duration.'p(95)'
$r.metrics.http_req_failed.value
$r.metrics.checks.value
```
