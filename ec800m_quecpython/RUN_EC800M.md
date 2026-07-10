# EC800M QuecPython 运行说明

## 4G 网络下后端地址怎么填写

EC800M 使用 SIM 卡 4G 上网时，不能把后端地址写成 `localhost` 或 `127.0.0.1`。这两个地址在开发板上表示“开发板自己”，不是你的 Windows 电脑。

大多数情况下也不能写电脑的 `192.168.x.x` 局域网地址。电脑的局域网地址只在同一个 Wi-Fi/有线局域网内可访问，而 EC800M 走运营商 4G 网络，不在你电脑的局域网里。

实训演示最推荐两种方式：

1. 内网穿透：例如 cpolar、ngrok、frp，把公网地址转发到本机 `8080`。
2. 公网服务器：把 Spring Boot 后端部署到有公网 IP 或域名的服务器。

如果使用内网穿透，本地后端仍然运行在：

```text
http://localhost:8080/api
```

穿透工具负责把公网地址转发到本机 `8080`。开发板配置里填写穿透后的公网地址。

当前先按后端默认 `8080` 端口提交，华为云服务器地址示例：

```python
SERVER_BASE_URL = "http://120.46.62.182:8080/api"
```

正式部署后由组长确认服务器端口。如果后续使用 Nginx 转发到 80 端口，地址可能改成：

```python
SERVER_BASE_URL = "http://120.46.62.182/api"
```

如果你的穿透工具给的是 HTTPS 地址，例如：

```text
https://abc123.cpolar.top
```

理论上配置应写成：

```python
SERVER_BASE_URL = "https://abc123.cpolar.top/api"
```

但当前 `ec800m_device_client.py` 使用资料里已有的 `usocket` 手写 HTTP POST，只支持 `http://`。实训演示阶段建议优先使用 `http://` 穿透地址，避免 HTTPS 证书和 `ussl` 兼容问题。后续确实需要 HTTPS 时，再单独增加 `ussl` 包装和证书校验处理。

注意：`SERVER_BASE_URL` 已经包含 `/api`，脚本里的 path 是：

```python
"/v1/device/heartbeat"
"/v1/device/sos"
```

最终拼出来应是：

```text
http://120.46.62.182:8080/api/v1/device/heartbeat
http://120.46.62.182:8080/api/v1/device/sos
```

不要写成：

```text
http://120.46.62.182:8080/api/api/v1/device/heartbeat
```

## 后端当前配置检查

当前 Spring Boot 配置为：

```yaml
server:
  port: 8080
  address: 0.0.0.0
  servlet:
    context-path: /api
```

结论：

- 后端端口是 `8080`。
- 接口统一带 `/api` 前缀。
- `server.address: 0.0.0.0` 已经允许监听所有网卡。
- 实际设备接口是：
  - `/api/v1/device/heartbeat`
  - `/api/v1/device/sos`
- Windows 防火墙仍可能拦截外部访问 `8080`。

## 本地验证流程

先启动后端：

```bat
cd /d D:\Desktop\SECP\SCEP-backend\backend
.\mvnw.cmd spring-boot:run
```

本机测试 heartbeat：

```bat
curl -X POST "http://localhost:8080/api/v1/device/heartbeat" ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceId\":\"ec800m-001\",\"secret\":\"demo-secret\",\"status\":\"online\",\"signalStrength\":21}"
```

本机测试 SOS：

```bat
curl -X POST "http://localhost:8080/api/v1/device/sos" ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceId\":\"ec800m-001\",\"secret\":\"demo-secret\",\"eventType\":\"HARDWARE_SOS\",\"area\":\"卧室床头区域\",\"message\":\"家庭硬件 SOS 被触发\"}"
```

如果由组长部署到华为云，确认后端已经在服务器 `8080` 端口启动；如果还是本机演示，则启动内网穿透，把公网地址转发到本机 `8080`。

当前按华为云后端默认端口测试：

```text
http://120.46.62.182:8080/api
```

测试服务器 heartbeat：

```bat
curl -X POST "http://120.46.62.182:8080/api/v1/device/heartbeat" ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceId\":\"ec800m-001\",\"secret\":\"demo-secret\",\"status\":\"online\",\"signalStrength\":21}"
```

测试服务器 SOS：

```bat
curl -X POST "http://120.46.62.182:8080/api/v1/device/sos" ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceId\":\"ec800m-001\",\"secret\":\"demo-secret\",\"eventType\":\"HARDWARE_SOS\",\"area\":\"卧室床头区域\",\"message\":\"家庭硬件 SOS 被触发\"}"
```

只有服务器或公网地址在电脑上测试成功后，才把它写进：

```text
D:\Desktop\SECP\SCEP-backend\ec800m_quecpython\ec800m_config.py
```

然后上传两个脚本到开发板 `/usr`：

```text
ec800m_config.py
ec800m_device_client.py
```

运行：

```python
example.exec('/usr/ec800m_device_client.py')
```

## 如何判断地址可用

按这个顺序判断：

1. `localhost:8080` 在电脑上 curl 成功。
2. 内网穿透公网地址在电脑上 curl 成功。
3. `ec800m_config.py` 填写公网地址，且只包含一个 `/api`。
4. 开发板网络检查显示 `4G network OK`。
5. 开发板串口打印 `HTTP status 200`。
6. 子女端前端刷新后看到设备在线或硬件 SOS。

如果电脑 curl 公网地址都失败，不要上开发板测试。先解决后端或穿透。

## 常见错误排查

### 404

- 检查是否漏了 `/api`。
- 检查是否重复写成 `/api/api`。
- 检查接口路径是否是 `/v1/device/heartbeat` 和 `/v1/device/sos`。

### 401

- 检查 `deviceId`。
- 检查 `secret`。
- 检查数据库 `secret_hash` 是否对应 `demo-secret`。

### 连接超时

- 检查穿透工具是否启动。
- 检查后端是否启动。
- 检查穿透是否转发到 `8080`。
- 检查 Windows 防火墙是否放行 `8080`。
- 检查开发板是否真的联网成功。

### 本机 localhost 可以，公网地址不可以

这是内网穿透配置问题，不要先怪开发板。先检查穿透工具和转发端口。

### 电脑公网地址可以，开发板不可以

- 检查开发板 SIM 卡和 4G 注册。
- 检查开发板 DNS 是否能解析穿透域名。
- 检查当前脚本是否误填 `https://`。
- 实训阶段优先尝试 `http://` 地址。
- 串口打印完整 URL 和异常信息，但不要打印 `secret`。
