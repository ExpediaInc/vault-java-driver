package com.bettercloud.vault.util;

import java.util.concurrent.TimeUnit;

public class TimeUtil {

    public static String timeString(Long time, TimeUnit timeUnit) {

        String unit;

        switch (timeUnit) {
            case MILLISECONDS:
                unit = "ms";
                break;
            case SECONDS:
                unit = "s";
                break;
            case MINUTES:
                unit = "m";
                break;
            case HOURS:
                unit = "h";
                break;
            default:
                throw new RuntimeException("Invalid time unit: " + timeUnit.toString());

        }

        return unit;
    }
}
