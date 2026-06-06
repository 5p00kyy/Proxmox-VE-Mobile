package com.proxmoxmobile.data.storage

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.StorageContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageRepositoryTest {
    @Test
    fun getStorages_filtersInvalidRowsAndSortsByStorageName() = runBlocking {
        val repository = StorageRepository(
            FakeStorageApi(
                storages = listOf(
                    storage(storage = "local-zfs", type = "zfspool"),
                    storage(storage = "", type = "dir"),
                    storage(storage = "backup-nfs", type = "nfs"),
                    storage(storage = "broken", type = ""),
                    storage(storage = "negative", available = -1)
                )
            )
        )

        val result = repository.getStorages("pve")

        assertTrue(result is StorageResult.Success)
        val storages = (result as StorageResult.Success).data
        assertEquals(listOf("backup-nfs", "local-zfs"), storages.map { it.storage })
    }

    @Test
    fun getStorages_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = StorageRepository(ProxmoxStorageApi { null })

        val result = repository.getStorages("pve")

        assertTrue(result is StorageResult.Error)
        assertEquals("Not authenticated", (result as StorageResult.Error).message)
    }

    @Test
    fun getStorages_reportsBlankNodeAsInvalidRequest() = runBlocking {
        val repository = StorageRepository(FakeStorageApi())

        val result = repository.getStorages(" ")

        assertTrue(result is StorageResult.Error)
        assertEquals("Node name is required", (result as StorageResult.Error).message)
    }

    @Test
    fun getStorageContent_filtersInvalidRowsAndSortsByTypeThenVolumeId() = runBlocking {
        val repository = StorageRepository(
            FakeStorageApi(
                content = listOf(
                    storageContent(volid = "local:iso/debian.iso", content = "iso"),
                    storageContent(volid = "", content = "iso"),
                    storageContent(volid = "local:backup/vzdump-qemu-100.vma.zst", content = "backup"),
                    storageContent(volid = "local:snippets/cloud.yaml", content = ""),
                    storageContent(volid = "local:iso/broken.iso", content = "iso", size = -1)
                )
            )
        )

        val result = repository.getStorageContent("pve", "local")

        assertTrue(result is StorageResult.Success)
        val content = (result as StorageResult.Success).data
        assertEquals(
            listOf(
                "local:backup/vzdump-qemu-100.vma.zst",
                "local:iso/debian.iso"
            ),
            content.map { it.volid }
        )
    }

    @Test
    fun getStorageContent_reportsBlankStorageAsInvalidRequest() = runBlocking {
        val repository = StorageRepository(FakeStorageApi())

        val result = repository.getStorageContent("pve", " ")

        assertTrue(result is StorageResult.Error)
        assertEquals("Storage name is required", (result as StorageResult.Error).message)
    }

    private class FakeStorageApi(
        private val storages: List<Storage> = listOf(storage()),
        private val content: List<StorageContent> = listOf(storageContent())
    ) : StorageApi {
        override suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>> {
            return ApiResponse(storages)
        }

        override suspend fun getStorageContent(
            nodeName: String,
            storageName: String
        ): ApiResponse<List<StorageContent>> {
            return ApiResponse(content)
        }
    }

    companion object {
        private fun storage(
            storage: String = "local",
            type: String = "dir",
            available: Long = 10L * 1024L * 1024L * 1024L
        ): Storage {
            return Storage(
                storage = storage,
                type = type,
                content = listOf("iso", "backup"),
                nodes = null,
                shared = false,
                active = true,
                available = available,
                used = 2L * 1024L * 1024L * 1024L,
                total = 12L * 1024L * 1024L * 1024L
            )
        }

        private fun storageContent(
            volid: String = "local:iso/debian.iso",
            content: String = "iso",
            size: Long? = 1024L * 1024L
        ): StorageContent {
            return StorageContent(
                volid = volid,
                content = content,
                size = size,
                format = "iso",
                ctime = 100,
                notes = null,
                vmid = null,
                used = null,
                parent = null,
                protectedContent = null
            )
        }
    }
}
