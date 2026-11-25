package com.hb.offlinechat.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayProtectionTest {

    @Test
    fun acceptsFreshMessageOnce() {
        val protection = ReplayProtection(windowMs = 60_000L)
        val now = System.currentTimeMillis()
        assertTrue(protection.isFresh("msg-1", now))
        assertFalse(protection.isFresh("msg-1", now))
    }

    @Test
    fun rejectsExpiredTimestamp() {
        val protection = ReplayProtection(windowMs = 1_000L)
        val stale = System.currentTimeMillis() - 10_000L
        assertFalse(protection.isFresh("msg-old", stale))
    }
}
