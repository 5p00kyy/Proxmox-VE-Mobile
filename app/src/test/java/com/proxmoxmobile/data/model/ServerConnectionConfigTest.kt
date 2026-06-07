package com.proxmoxmobile.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ServerConnectionConfigTest {
    @Test
    fun normalizedForConnection_keepsBareHostOnHttpsDefaultPort() {
        val config = serverConfig(host = "192.0.2.10")

        val normalized = config.normalizedForConnection()

        assertEquals("192.0.2.10", normalized.host)
        assertEquals(8006, normalized.port)
        assertEquals(true, normalized.useHttps)
    }

    @Test
    fun normalizedForConnection_extractsHttpsUrlHostAndPort() {
        val config = serverConfig(host = "https://192.0.2.10:8006/api2/json", port = 1234, useHttps = false)

        val normalized = config.normalizedForConnection()

        assertEquals("192.0.2.10", normalized.host)
        assertEquals(8006, normalized.port)
        assertEquals(true, normalized.useHttps)
    }

    @Test
    fun normalizedForConnection_rejectsCleartextLanHosts() {
        val error = expectIllegalArgument {
            serverConfig(host = "http://192.0.2.10:8006", useHttps = false).normalizedForConnection()
        }

        assertEquals(
            "HTTP is only supported for localhost or Android emulator test endpoints. Use HTTPS with port 8006 for Proxmox hosts.",
            error.message
        )
    }

    @Test
    fun normalizedForConnection_upgradesPastedHttpUrlWhenHttpsIsEnabled() {
        val config = serverConfig(host = "http://192.0.2.10:8006", useHttps = true)

        val normalized = config.normalizedForConnection()

        assertEquals("192.0.2.10", normalized.host)
        assertEquals(8006, normalized.port)
        assertEquals(true, normalized.useHttps)
    }

    @Test
    fun normalizedForConnection_allowsCleartextEmulatorLoopback() {
        val config = serverConfig(host = "http://10.0.2.2:8006", useHttps = false)

        val normalized = config.normalizedForConnection()

        assertEquals("10.0.2.2", normalized.host)
        assertEquals(8006, normalized.port)
        assertEquals(false, normalized.useHttps)
    }

    @Test
    fun normalizedForConnection_rejectsInvalidPorts() {
        val error = expectIllegalArgument {
            serverConfig(host = "192.0.2.10", port = 0).normalizedForConnection()
        }

        assertEquals("Port must be between 1 and 65535", error.message)
    }

    private fun serverConfig(
        host: String,
        port: Int = 8006,
        useHttps: Boolean = true
    ): ServerConfig {
        return ServerConfig(
            host = host,
            port = port,
            username = "tester",
            password = "secret",
            realm = "pam",
            useHttps = useHttps,
            verifySsl = true
        )
    }

    private fun expectIllegalArgument(block: () -> Unit): IllegalArgumentException {
        return try {
            block()
            fail("Expected IllegalArgumentException")
            error("Unreachable")
        } catch (e: IllegalArgumentException) {
            e
        }
    }
}
