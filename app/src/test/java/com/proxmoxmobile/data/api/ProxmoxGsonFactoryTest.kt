package com.proxmoxmobile.data.api

import com.google.gson.reflect.TypeToken
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.NetworkInterface
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxmoxGsonFactoryTest {
    private val gson = ProxmoxGsonFactory.create()

    @Test
    fun parsesStorageContentStringAndNumericFlags() {
        val responseType = object : TypeToken<ApiResponse<List<Storage>>>() {}.type
        val json = """
            {
              "data": [
                {
                  "storage": "local",
                  "type": "dir",
                  "content": "iso,vztmpl,backup",
                  "shared": 0,
                  "active": 1,
                  "avail": 1073741824,
                  "used": 536870912,
                  "total": 1610612736
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson<ApiResponse<List<Storage>>>(json, responseType)
        val storage = response.data.single()

        assertEquals("local", storage.storage)
        assertEquals(listOf("iso", "vztmpl", "backup"), storage.content)
        assertFalse(storage.shared)
        assertTrue(storage.active)
        assertEquals(1073741824L, storage.available)
        assertEquals(536870912L, storage.used)
        assertEquals(1610612736L, storage.total)
    }

    @Test
    fun parsesStorageArrayContentAndStringFlags() {
        val storage = gson.fromJson(
            """
                {
                  "storage": "backup-nfs",
                  "type": "nfs",
                  "content": ["backup", "images"],
                  "nodes": "pve-a,pve-b",
                  "shared": "true",
                  "enabled": "1",
                  "available": 2048,
                  "used": 1024,
                  "total": 4096
                }
            """.trimIndent(),
            Storage::class.java
        )

        assertEquals(listOf("backup", "images"), storage.content)
        assertEquals(listOf("pve-a", "pve-b"), storage.nodes)
        assertTrue(storage.shared)
        assertTrue(storage.active)
        assertEquals(2048L, storage.available)
    }

    @Test
    fun parsesUserNumericEnableFlagAndNullableProfileFields() {
        val responseType = object : TypeToken<ApiResponse<List<User>>>() {}.type
        val json = """
            {
              "data": [
                {
                  "userid": "backup@pve",
                  "enable": 1,
                  "expire": 0,
                  "firstname": null,
                  "lastname": "Operator",
                  "email": null,
                  "comment": "scheduled backups"
                },
                {
                  "userid": "disabled@pve",
                  "enable": 0
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson<ApiResponse<List<User>>>(json, responseType)

        assertEquals("backup@pve", response.data[0].userid)
        assertTrue(response.data[0].enable)
        assertEquals(0L, response.data[0].expire)
        assertEquals(null, response.data[0].firstname)
        assertEquals("Operator", response.data[0].lastname)
        assertEquals(null, response.data[0].email)
        assertEquals("scheduled backups", response.data[0].comment)
        assertEquals("disabled@pve", response.data[1].userid)
        assertFalse(response.data[1].enable)
    }

    @Test
    fun parsesUserStringEnableFlag() {
        val user = gson.fromJson(
            """
                {
                  "userid": "readonly@pve",
                  "enable": "true",
                  "expire": "1893456000"
                }
            """.trimIndent(),
            User::class.java
        )

        assertEquals("readonly@pve", user.userid)
        assertTrue(user.enable)
        assertEquals(1893456000L, user.expire)
    }

    @Test
    fun parsesNetworkNumericFlagsAndOptionalFamilies() {
        val responseType = object : TypeToken<ApiResponse<List<NetworkInterface>>>() {}.type
        val json = """
            {
              "data": [
                {
                  "iface": "vmbr0",
                  "type": "bridge",
                  "method": "static",
                  "active": 1,
                  "autostart": 1,
                  "exists": 1,
                  "families": "inet,inet6"
                },
                {
                  "iface": "tap100i0",
                  "type": "unknown",
                  "active": 0,
                  "autostart": 0
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson<ApiResponse<List<NetworkInterface>>>(json, responseType)

        assertEquals("vmbr0", response.data[0].iface)
        assertTrue(response.data[0].active)
        assertTrue(response.data[0].autostart)
        assertTrue(response.data[0].exists)
        assertEquals(listOf("inet", "inet6"), response.data[0].families)
        assertEquals("tap100i0", response.data[1].iface)
        assertFalse(response.data[1].active)
        assertFalse(response.data[1].autostart)
        assertTrue(response.data[1].exists)
        assertEquals(emptyList<String>(), response.data[1].families)
    }
}
