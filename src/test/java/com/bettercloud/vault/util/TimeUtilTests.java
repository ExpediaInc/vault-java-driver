package com.bettercloud.vault.util;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TimeUtilTests {

    @Test
    public void testTimeString() {
        assertEquals("10s", TimeUtil.timeString(10L, TimeUnit.SECONDS));
        assertEquals("100m", TimeUtil.timeString(100L, TimeUnit.MINUTES));
        assertEquals("200h", TimeUtil.timeString(200L, TimeUnit.HOURS));
    }

    @Test(expected = RuntimeException.class)
    public void testTimeString_badUnit() {
        TimeUtil.timeString(10L, TimeUnit.DAYS);
    }
}
