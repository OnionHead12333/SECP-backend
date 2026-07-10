# -*- coding: utf-8 -*-

import checkNet
import gc
import net
import sim
import usocket
import utime
from machine import Pin

from ec800m_config import DEVICE_ID
from ec800m_config import DEVICE_SECRET
from ec800m_config import INSTALL_AREA
from ec800m_config import SERVER_BASE_URL


HEARTBEAT_INTERVAL_SECONDS = 30
# 缩短超时，避免某次请求长时间阻塞导致“卡死”观感。
HTTP_TIMEOUT_SECONDS = 10
SOS_HTTP_RETRY_COUNT = 2
SOS_HTTP_RETRY_DELAY_SECONDS = 2
SOS_BUTTON_DEBOUNCE_SECONDS = 5
# Poll buttons instead of ExtInt interrupts. This avoids button bounce causing
# interrupt storms and serial disconnects.
# GPIO27 = onboard KEY. GPIO7 = external button between IO7 and GND.
# Idle reads 1 with pull-up. Pressed reads 0.
SOS_BUTTONS = (
    ("KEY/GPIO27", Pin.GPIO27),
    ("IO7/GPIO7", Pin.GPIO7),
)
SOS_BUTTON_STABLE_MS = 150

_last_sos_at = 0
_sos_buttons = []


def _strip_right_slash(value):
    while value.endswith("/"):
        value = value[:-1]
    return value


def _json_escape(value):
    text = str(value)
    text = text.replace("\\", "\\\\")
    text = text.replace('"', '\\"')
    text = text.replace("\r", "\\r")
    text = text.replace("\n", "\\n")
    text = text.replace("\t", "\\t")
    return text


def _json_value(value):
    if value is None:
        return "null"
    if value is True:
        return "true"
    if value is False:
        return "false"
    if isinstance(value, int):
        return str(value)
    return '"' + _json_escape(value) + '"'


def _json_dumps(data):
    parts = []
    for key in data:
        parts.append('"' + _json_escape(key) + '":' + _json_value(data[key]))
    return "{" + ",".join(parts) + "}"


def _parse_http_url(url):
    if not url.startswith("http://"):
        raise ValueError("only http:// is supported")
    rest = url[7:]
    slash = rest.find("/")
    if slash < 0:
        host_port = rest
        path = "/"
    else:
        host_port = rest[:slash]
        path = rest[slash:]
    colon = host_port.find(":")
    if colon >= 0:
        host = host_port[:colon]
        port = int(host_port[colon + 1:])
    else:
        host = host_port
        port = 80
    if not host:
        raise ValueError("empty host")
    return host, port, path


def _decode_response(raw):
    try:
        return raw.decode("utf-8")
    except Exception:
        try:
            return raw.decode()
        except Exception:
            return str(raw)


def _http_post(path, body_dict):
    base = _strip_right_slash(SERVER_BASE_URL)
    url = base + path
    host, port, request_path = _parse_http_url(url)
    body = _json_dumps(body_dict)
    body_bytes = body.encode("utf-8")
    debug_body = {}
    for key in body_dict:
        debug_body[key] = "***" if key == "secret" else body_dict[key]

    print("HTTP POST", url)
    print("HTTP body", _json_dumps(debug_body))

    addr = usocket.getaddrinfo(host, port)[0][-1]
    sock = usocket.socket(usocket.AF_INET, usocket.SOCK_STREAM)
    try:
        try:
            sock.settimeout(HTTP_TIMEOUT_SECONDS)
        except Exception:
            pass

        sock.connect(addr)
        header = (
            "POST " + request_path + " HTTP/1.1\r\n"
            "Host: " + host + "\r\n"
            "Content-Type: application/json; charset=utf-8\r\n"
            "Content-Length: " + str(len(body_bytes)) + "\r\n"
            "Connection: close\r\n"
            "\r\n"
        )
        sock.send(header.encode("utf-8"))
        sock.send(body_bytes)

        chunks = []
        while True:
            try:
                chunk = sock.recv(512)
            except Exception:
                break
            if not chunk:
                break
            chunks.append(chunk)

        raw = b""
        for chunk in chunks:
            raw += chunk
        text = _decode_response(raw)
        first_line_end = text.find("\r\n")
        first_line = text[:first_line_end] if first_line_end >= 0 else text
        status_code = -1
        fields = first_line.split(" ")
        if len(fields) >= 2:
            try:
                status_code = int(fields[1])
            except Exception:
                status_code = -1

        body_start = text.find("\r\n\r\n")
        response_body = text[body_start + 4:] if body_start >= 0 else text
        print("HTTP status", status_code)
        print("HTTP response", response_body)
        return status_code, response_body
    except Exception as e:
        print("HTTP error", e)
        return -1, str(e)
    finally:
        try:
            sock.close()
        except Exception:
            pass
        # 每次请求后回收内存，防止长时间运行后堆碎片化导致卡死。
        try:
            gc.collect()
        except Exception:
            pass


def _config_ready():
    if "你的后端地址" in SERVER_BASE_URL:
        print("CONFIG ERROR: please edit SERVER_BASE_URL in ec800m_config.py")
        print("Example: http://public-ip:8080/api")
        return False
    if SERVER_BASE_URL.startswith("https://"):
        print("CONFIG ERROR: current script only supports http://")
        print("Use an http tunnel address, or add ussl HTTPS support later.")
        return False
    if not SERVER_BASE_URL.startswith("http://"):
        print("CONFIG ERROR: SERVER_BASE_URL must start with http://")
        return False
    return True


def check_sim_and_network():
    print("EC800M network check start")
    try:
        imsi = sim.getImsi()
    except Exception as e:
        print("SIM error", e)
        return False

    print("IMSI", imsi)
    if not imsi:
        print("SIM not ready")
        return False

    print("waiting 4G network...")
    stagecode, subcode = checkNet.wait_network_connected(60)
    print("stagecode =", stagecode, "subcode =", subcode)
    if stagecode == 3 and subcode == 1:
        print("4G network OK")
        return True

    print("4G network not ready")
    try:
        print("net state", net.getState())
    except Exception as e:
        print("net.getState failed", e)
    return False


def get_signal_strength():
    try:
        if hasattr(net, "csqQueryPoll"):
            value = net.csqQueryPoll()
            print("signal raw", value)
            if isinstance(value, tuple) or isinstance(value, list):
                if len(value) > 0:
                    return int(value[0])
            return int(value)
    except Exception as e:
        print("signalStrength read failed", e)
    return -1


def send_heartbeat():
    payload = {
        "deviceId": DEVICE_ID,
        "secret": DEVICE_SECRET,
        "status": "online",
        "signalStrength": get_signal_strength(),
    }
    return _http_post("/v1/device/heartbeat", payload)


def send_sos():
    payload = {
        "deviceId": DEVICE_ID,
        "secret": DEVICE_SECRET,
        "eventType": "HARDWARE_SOS",
        "area": INSTALL_AREA,
        "message": "family hardware SOS triggered",
    }
    last_status = -1
    last_response = ""
    for attempt in range(1, SOS_HTTP_RETRY_COUNT + 1):
        print("SOS upload attempt", attempt)
        last_status, last_response = _http_post("/v1/device/sos", payload)
        if last_status >= 200 and last_status < 300:
            return last_status, last_response
        if attempt < SOS_HTTP_RETRY_COUNT:
            print("SOS upload retry after", SOS_HTTP_RETRY_DELAY_SECONDS, "seconds")
            utime.sleep(SOS_HTTP_RETRY_DELAY_SECONDS)
    return last_status, last_response


def _read_pin(pin):
    try:
        return pin.read()
    except Exception:
        try:
            return pin.value()
        except Exception:
            return pin()


def setup_sos_buttons():
    global _sos_buttons
    _sos_buttons = []
    for name, gpio in SOS_BUTTONS:
        try:
            # Pin.IN mode does not need an initial level argument.
            pin = Pin(gpio, Pin.IN, Pin.PULL_PU)
            value = _read_pin(pin)
            button = {
                "name": name,
                "pin": pin,
                "low_since": None,
                "latched": False,
                # A button must be seen released once before it can trigger SOS.
                # This prevents a stuck/incorrect wire from uploading SOS on boot.
                "armed": value != 0,
            }
            _sos_buttons.append(button)
            print("SOS button ready:", name, "read once =", value, "armed =", button["armed"])
        except Exception as e:
            print("SOS button setup failed:", name, e)
    return len(_sos_buttons) > 0


def _handle_sos_request():
    global _last_sos_at

    if not _sos_buttons:
        return

    now_ms = utime.ticks_ms()
    for button in _sos_buttons:
        value = _read_pin(button["pin"])
        name = button["name"]

        if value != 0:
            if not button["armed"]:
                print("SOS button armed:", name)
            button["armed"] = True
            button["low_since"] = None
            button["latched"] = False
            continue

        if not button["armed"]:
            continue

        if button["low_since"] is None:
            button["low_since"] = now_ms
            print(name, "low detected, hold to confirm...")
            continue

        if button["latched"]:
            continue

        if utime.ticks_diff(now_ms, button["low_since"]) < SOS_BUTTON_STABLE_MS:
            continue

        button["latched"] = True
        now = utime.time()
        if _last_sos_at and now - _last_sos_at < SOS_BUTTON_DEBOUNCE_SECONDS:
            print("SOS ignored: debounce", name)
            continue

        _last_sos_at = now
        print("SOS button pressed:", name, "uploading...")
        send_sos()


def start():
    if not _config_ready():
        return

    while not check_sim_and_network():
        print("network check failed, retry after 10 seconds")
        utime.sleep(10)

    setup_sos_buttons()
    print("heartbeat loop start")
    last_heartbeat_at = 0
    loop_count = 0
    while True:
        # 任何单次异常都不允许让脚本退出 / 卡死。
        try:
            _handle_sos_request()
        except Exception as e:
            print("SOS handle error", e)

        now = utime.time()
        if last_heartbeat_at == 0 or now - last_heartbeat_at >= HEARTBEAT_INTERVAL_SECONDS:
            try:
                send_heartbeat()
            except Exception as e:
                print("heartbeat error", e)
            last_heartbeat_at = now

        # 每隔约 5 秒打印一次，证明脚本还在运行（顺便回收内存）。
        loop_count += 1
        if loop_count % 50 == 0:
            try:
                gc.collect()
                print("alive tick", loop_count, "free", gc.mem_free())
            except Exception:
                print("alive tick", loop_count)

        utime.sleep_ms(100)


if __name__ == "__main__":
    start()
