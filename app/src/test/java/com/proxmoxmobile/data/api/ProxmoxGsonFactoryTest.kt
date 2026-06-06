package com.proxmoxmobile.data.api

import com.google.gson.reflect.TypeToken
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Storage
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
}
