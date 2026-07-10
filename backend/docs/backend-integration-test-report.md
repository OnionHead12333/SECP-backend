# SECP 智慧养老平台后端集成测试报告

日期：2026-06-06

## 1. 测试概念说明

单元测试：只测一个很小的代码单元，例如某个 Service 方法。通常会用 Mockito 模拟数据库或依赖，优点是快，缺点是不能证明 Controller、Security、JPA、数据库真的能一起工作。

集成测试：把多个真实模块串起来测。本次采用 `SpringBootTest + MockMvc`，会启动 Spring Boot 容器、Controller、Spring Security、JWT Filter、Service、JPA，并连接本地 MySQL。它不需要启动真实 HTTP 端口，但请求会经过接近真实后端的调用链。

接口冒烟测试：只快速检查接口是否能访问、是否返回大致正常，通常覆盖少、断言浅。它适合上线前快速检查，但不能替代集成测试。

## 2. 技术方案

采用方案：`SpringBootTest + AutoConfigureMockMvc + 本地 MySQL`

原因：

- 项目是 Spring Boot 3.2.5，已有 `spring-boot-starter-test`，天然支持 MockMvc。
- 项目使用 Spring Security + JWT，MockMvc 可以真实经过安全过滤器。
- 项目使用 JPA + MySQL，本次测试连接 `application.yml` 的本地 `dev` 数据源：`jdbc:mysql://localhost:3306/elder`。
- 集成测试类命名为 `BackendIntegrationIT`，默认 `mvnw test` 不自动运行它，需要显式执行，避免影响普通单元测试速度。
- 测试类使用 `@Transactional`，测试数据在每个测试结束后回滚，降低污染本地数据库的风险。

## 3. 测试范围

本轮后端集成测试覆盖：

- 老人注册
- 子女注册并绑定老人
- 老人登录
- 子女登录
- 子女查看绑定老人
- 子女创建喝水提醒
- 子女查看喝水提醒
- 老人上传定位
- 子女查看定位摘要
- 老人发起 SOS
- 老人查看 SOS
- 错误密码登录
- 未登录访问私有接口
- 老人访问子女接口
- 子女访问老人接口
- 非法参数校验

## 4. 测试环境

- 后端目录：`D:\BJTU6\SECP\SCEP-backend\backend`
- 框架：Spring Boot 3.2.5
- Java 编译目标：Java 17
- 本次执行 JVM：Java 21.0.8
- 构建工具：Maven Wrapper
- 数据库：本地 MySQL，数据库 `elder`
- 测试框架：JUnit 5、Spring Boot Test、MockMvc
- 安全链路：Spring Security、JWT Bearer Token

## 5. 测试用例表

| 编号 | 用例 | 接口/链路 | 断言重点 | 结果 |
| --- | --- | --- | --- | --- |
| IT-01 | 注册老人 | `POST /v1/auth/register` | `code=0`，角色为 `elder` | 通过 |
| IT-02 | 注册子女并绑定老人 | `POST /v1/auth/register-child-with-elders` | `code=0`，角色为 `child`，`familyCount=1` | 通过 |
| IT-03 | 老人登录 | `POST /v1/auth/login` | 返回 JWT token | 通过 |
| IT-04 | 子女登录 | `POST /v1/auth/login` | 返回 JWT token | 通过 |
| IT-05 | 子女查看绑定老人 | `GET /v1/child/bound-elders` | 返回绑定老人手机号和 `elderProfileId` | 通过 |
| IT-06 | 子女创建喝水提醒 | `POST /v1/child/water-reminders` | 创建成功，返回提醒 ID | 通过 |
| IT-07 | 子女查看喝水提醒 | `GET /v1/child/water-reminders` | 列表包含刚创建的提醒 | 通过 |
| IT-08 | 老人上传定位 | `POST /v1/elder/location-tracks` | 上传成功，返回 `locationId` | 通过 |
| IT-09 | 子女查看定位摘要 | `GET /v1/child/elders/{elderId}/location-summary` | 经纬度与老人上传值一致 | 通过 |
| IT-10 | 老人发起 SOS | `POST /v1/elder/emergency-alerts` | `status=pending_revoke`，返回 `alertId` | 通过 |
| IT-11 | 老人查看 SOS | `GET /v1/elder/emergency-alerts/{alertId}` | 返回刚创建的 SOS | 通过 |
| IT-12 | 错误密码登录 | `POST /v1/auth/login` | 业务失败码 `401` | 通过 |
| IT-13 | 未登录访问私有接口 | `GET /v1/child/bound-elders` | 业务失败码 `4010` | 通过 |
| IT-14 | 老人访问子女接口 | `GET /v1/child/bound-elders` | 业务失败码 `4030` | 通过 |
| IT-15 | 子女访问老人接口 | `POST /v1/elder/location-tracks` | 业务失败码 `4030` | 通过 |
| IT-16 | 非法参数校验 | `POST /v1/auth/register`，非法手机号 | 校验失败码 `4000` | 通过 |

## 6. 新增/调整文件

- 新增：`src/test/java/com/smartelderly/integration/BackendIntegrationIT.java`
- 调整：`src/test/java/com/smartelderly/service/EmergencyAlertServiceTest.java`
  - 原因：分页字段是数字，但测试用 `Integer`/`Long` 精确装箱类型比较，导致语义正确但断言失败。
  - 改法：新增 `assertNumberEquals`，统一按 `Number.longValue()` 比较。

## 7. 执行命令

运行普通单元测试：

```powershell
.\mvnw.cmd test
```

运行本轮后端集成测试：

```powershell
.\mvnw.cmd "-Dtest=com.smartelderly.integration.BackendIntegrationIT" test
```

注意：在 PowerShell 中，`-Dtest=...` 建议加引号，否则参数可能被错误解析。

## 8. 执行结果

普通测试：

- 命令：`.\mvnw.cmd test`
- 结果：`Tests run: 45, Failures: 0, Errors: 0, Skipped: 0`
- 构建结果：`BUILD SUCCESS`

集成测试：

- 命令：`.\mvnw.cmd "-Dtest=com.smartelderly.integration.BackendIntegrationIT" test`
- 结果：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
- 构建结果：`BUILD SUCCESS`

## 9. 发现的问题

1. 本地 Maven 配置文件有警告：
   - 文件：`C:\Users\user\.m2\settings.xml`
   - 现象：Maven 报 `expected START_TAG or END_TAG not TEXT`。
   - 影响：目前不阻塞构建，但说明 settings.xml 里可能混入了普通命令文本，后续建议清理。

2. `EmergencyAlertServiceTest` 原有数字断言过窄：
   - 现象：`1L` 与 `1` 数值相同，但 Java 装箱类型不同导致断言失败。
   - 已处理：改成按 `Number.longValue()` 比较。

3. 后端统一响应风格需要注意：
   - 多数业务错误返回 HTTP 200，但 JSON 中 `code` 表示失败，例如 `4010`、`4030`、`4000`。
   - 因此测试不能只看 HTTP 状态，还要断言 JSON `code`。

4. 定位摘要接口权限值得后续加强复查：
   - `ChildLocationSummaryController` 当前校验了子女角色。
   - 是否强制校验“该子女绑定了该老人”，需要继续从 Service 层或后续测试中加深验证。

## 10. 未覆盖风险

- 没有覆盖真实 HTTP 端口启动后的外部调用链，当前是 MockMvc 层集成测试。
- 没有覆盖前端联调、跨域、文件上传、OCR、AI、社区、医疗档案等模块。
- 没有覆盖并发注册、重复手机号、重复绑定、SOS 自动发送定时任务等复杂边界。
- 没有覆盖数据库迁移脚本与全新空库初始化过程，只验证了当前本地 `elder` 库可运行。
- 没有覆盖定位权限开关、围栏、活动异常等更细分的位置守护逻辑。

