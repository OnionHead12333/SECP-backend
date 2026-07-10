# -*- coding: utf-8 -*-
#
# EC800M autostart entry.
#
# Boot behavior:
# 1. After power on, wait about 3 seconds.
# 2. During these 3 seconds, hold the onboard KEY/GPIO27 for about 1 second
#    to cancel autostart and stay in REPL.
# 3. If KEY is not held, start the heartbeat loop and SOS button polling.
#
# SOS inputs are configured in ec800m_device_client.py:
# - onboard KEY/GPIO27
# - external button IO7/GPIO7 to GND

import sys
import utime
from machine import Pin

try:
    sys.path.append("/usr")
except Exception:
    pass

print("EC800M booting... hold KEY within 3s to cancel autostart")

cancel = False
try:
    key = Pin(Pin.GPIO27, Pin.IN, Pin.PULL_PU)
    held = 0
    for _ in range(30):
        try:
            value = key.read()
        except Exception:
            value = 1

        if value == 0:
            held += 1
        else:
            held = 0

        if held >= 10:
            cancel = True
            break

        utime.sleep_ms(100)
except Exception as e:
    print("boot key check error:", e)

if cancel:
    print("autostart cancelled, staying in REPL")
else:
    try:
        import ec800m_device_client

        ec800m_device_client.start()
    except Exception as e:
        print("EC800M autostart failed:", e)
