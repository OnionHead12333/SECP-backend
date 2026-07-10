# SECP backend local k6 tests

这些脚本只用于本地 Spring Boot + 本地 MySQL 压测。它们可以帮助你发现接口慢点、并发错误、业务断言失败，但不能证明线上容量，因为线上机器、网络、数据库规格、连接池、日志级别、网关和外部依赖都会不同。

## 1. 三种测试怎么区分

负载测试：给系统一个“预计会遇到的正常流量”，看它是否稳定。例如 30 个虚拟用户持续 5 分钟登录并读取绑定老人。它回答的是：正常使用压力下稳不稳。

压力测试：逐步加大流量，直到响应时间明显变慢、错误率升高或资源顶不住。它回答的是：本地环境大概从哪里开始吃力。

稳定性测试：用中低压力长时间运行，例如 10 到 30 个 VU 跑 30 分钟或 1 小时，观察是否内存涨、连接泄漏、数据库慢慢变慢。它回答的是：长时间运行会不会累积问题。

## 2. 启动后端

打开第一个 PowerShell：

```powershell
cd D:\BJTU6\SECP\SCEP-backend\backend
.\mvnw.cmd spring-boot:run
```

后端默认使用 `dev` profile，端口是 `8080`，接口前缀是 `http://localhost:8080/api`。

如果提示 MySQL 连不上，先确认本地 MySQL 已启动，库名是 `elder`，账号密码与 `src\main\resources\application.yml` 一致。

## 3. 确认 k6 可用

打开第二个 PowerShell：

```powershell
k6 version
```

如果提示找不到 `k6`，关闭并重新打开 PowerShell；还不行就把 k6 安装目录加入 PATH。

## 4. 本地推荐场景

| 场景 | 脚本 | 默认 VUs | 默认 duration | 类型 | 说明 |
|---|---:|---:|---:|---|---|
| 健康检查 | `health-smoke.js` | 50 | 3m | 纯读 | 最轻量基线 |
| 登录 + 绑定老人读取 | `login-bound-elders.js` | 30 | 5m | 登录 + 读 | 覆盖 JWT、用户查询、家庭绑定查询 |
| 老人定位上传 | `location-upload.js` | 30 | 5m | 写 | setup 先预热一次，避免首次并发初始化干扰 |
| 喝水提醒创建 + 查询 | `water-reminder-create-query.js` | 10 | 3m | 少量写 + 读 | 每个 VU 只创建一个提醒，后续查询 |
| SOS 发起 + 查看 | `sos-create-view.js` | 10 | 3m | 少量写 + 读 | 老人创建并 send-now，老人详情和子女列表读取 |
| 子女告警列表读取 | `child-alerts-list.js` | 20 | 3m | 读 | setup 造少量已发送告警，正式阶段只读列表 |
| 医疗档案轻量读取 | `medical-archive-read.js` | 15 | 3m | 少量写 + 读 | setup 建文件夹，正式阶段读取文件夹和文档列表，不调用 OCR/AI |

## 5. 运行命令

先进入后端目录：

```powershell
cd D:\BJTU6\SECP\SCEP-backend\backend
```

健康检查：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api --summary-export load-test\reports\health.json load-test\k6\health-smoke.js
```

登录 + 读取绑定老人：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api --summary-export load-test\reports\login-bound-elders.json load-test\k6\login-bound-elders.js
```

老人定位上传：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api --summary-export load-test\reports\location-upload.json load-test\k6\location-upload.js
```

喝水提醒创建 + 查询：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api --summary-export load-test\reports\water-reminder-create-query.json load-test\k6\water-reminder-create-query.js
```

SOS 发起 + 查看：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api --summary-export load-test\reports\sos-create-view.json load-test\k6\sos-create-view.js
```

子女告警列表读取：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api --summary-export load-test\reports\child-alerts-list.json load-test\k6\child-alerts-list.js
```

医疗档案轻量读取：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api --summary-export load-test\reports\medical-archive-read.json load-test\k6\medical-archive-read.js
```

短时间试跑可以覆盖默认值：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api -e VUS=3 -e DURATION=30s --summary-export load-test\reports\water-reminder-create-query-short.json load-test\k6\water-reminder-create-query.js
```

## 6. 怎么看结果

k6 终端里重点看：

- `http_req_duration p(95)`：95% 请求都不超过这个耗时，常叫 P95。
- `http_req_failed`：HTTP 层错误率，例如 4xx/5xx 或请求失败。
- `http_reqs` rate：每秒请求数，也就是 RPS。
- `checks`：脚本里的业务断言通过率，例如 `code === 0`、返回 token、列表包含数据。
- `iterations`：业务流程执行了多少轮。一个 iteration 可能包含多个 HTTP 请求。

也可以从 JSON 摘关键值：

```powershell
$r = Get-Content load-test\reports\health.json | ConvertFrom-Json
$r.metrics.http_req_duration.'p(95)'
$r.metrics.http_req_failed.value
$r.metrics.http_reqs.rate
$r.metrics.checks.value
```

## 7. 业务断言失败怎么判断

先分清两层：

HTTP 错误：`http_req_failed` 大于 0，说明请求层面失败，例如 500、连接失败、超时。

业务断言失败：HTTP 可能是 200，但 `checks` 失败，例如接口返回 `code != 0`、没有 token、列表为空、没有返回 id。

判断步骤：

1. 看失败的 check 名字。比如 `location upload code is 0` 失败，说明接口返回了业务错误，不是网络错误。
2. 降低并发复现：用 `-e VUS=1 -e DURATION=30s` 跑。如果单并发也失败，更像接口或测试数据问题。
3. 换成每个 VU 独立数据。如果独立数据正常、共享数据失败，通常是脚本数据竞争。
4. 看业务规则。比如 SOS 在撤销窗口内只能有一个 `pending_revoke`，并发重复创建返回 existing pending alert 是合理业务限制。
5. 看后端日志。重点搜异常栈、`ApiException`、数据库唯一约束、连接池耗尽、事务冲突。
6. 判断脚本是否设计合理。比如本地压测不应该把 OCR、语音、AI 外部服务纳入后端基础压测，否则测到的是外部服务和网络。

这次定位上传脚本已经做了一个修正：setup 先上传一次定位，提前初始化定位守护设置，减少首次并发创建设置导致的业务失败。

## 8. 本地阶梯压测建议

先跑默认值确认功能，再逐步加压：

```powershell
k6 run -e BASE_URL=http://localhost:8080/api -e VUS=10 -e DURATION=2m --summary-export load-test\reports\health-10vu.json load-test\k6\health-smoke.js
k6 run -e BASE_URL=http://localhost:8080/api -e VUS=30 -e DURATION=2m --summary-export load-test\reports\health-30vu.json load-test\k6\health-smoke.js
k6 run -e BASE_URL=http://localhost:8080/api -e VUS=50 -e DURATION=2m --summary-export load-test\reports\health-50vu.json load-test\k6\health-smoke.js
```

如果 P95、错误率、checks 都稳定，再对业务接口做类似阶梯。写入型脚本建议不要盲目加到很大，避免产生大量测试数据。
