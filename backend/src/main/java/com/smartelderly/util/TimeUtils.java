package com.smartelderly.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeUtils {

    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private TimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(APP_ZONE);
    }

    public static Instant toInstant(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.atZone(APP_ZONE).toInstant();
    }
}
