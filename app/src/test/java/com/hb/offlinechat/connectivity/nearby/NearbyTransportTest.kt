package com.hb.offlinechat.connectivity.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearbyTransportTest {

    @Test
    fun endpointNameRoundTrip() {
        val encoded = NearbyTransport.encodeEndpointName("dev123", "HB-A1B2", "publicKeyBase64")
        val parsed = NearbyTransport.parseEndpointName(encoded)
        assertEquals("dev123", parsed?.deviceId)
        assertEquals("HB-A1B2", parsed?.deviceName)
        assertEquals("publicKeyBase64", parsed?.publicKey)
    }

    @Test
    fun foreignEndpointNameIsIgnored() {
        assertNull(NearbyTransport.parseEndpointName("SomeOtherApp|dev|name|key"))
    }

    @Test
    fun malformedEndpointNameIsIgnored() {
        assertNull(NearbyTransport.parseEndpointName("HBCHAT|onlytwo"))
    }
}
