# -*- coding: utf-8 -*-

DEVICE_ID = "ec800m-001"
# 实训演示密钥；正式环境需要更换，并在数据库中只保存 secret_hash。
DEVICE_SECRET = "demo-secret"

# EC800M 使用 SIM 卡 4G 访问后端时，不能填写 localhost / 127.0.0.1。
# 通常也不能填写电脑的 192.168.x.x 局域网地址。
# 请填写公网服务器地址或内网穿透地址。
# 当前先按后端默认 8080 端口提交；正式部署后由组长确认服务器端口。
# 如果后续使用 Nginx 转发到 80 端口，地址可能改成：
# SERVER_BASE_URL = "http://120.46.62.182/api"
# 注意：SERVER_BASE_URL 末尾已经包含 /api，脚本 path 不要再写 /api。
# 当前 ec800m_device_client.py 使用 usocket，默认只支持 http://。
# 如果穿透工具只给 https://，实训演示阶段优先换 http:// 穿透地址，避免证书问题。
SERVER_BASE_URL = "http://120.46.62.182:8080/api"

INSTALL_AREA = "卧室床头区域"
