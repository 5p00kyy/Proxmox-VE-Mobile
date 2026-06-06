package com.proxmoxmobile.data.backup

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.StorageContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {
    @Test
    fun getBackupInventory_filtersBackupStorageAndInvalidRows() = runBlocking {
        val api = FakeBackupApi(
            storages = listOf(
                storage(storage = "iso-store", content = listOf("iso")),
                storage(storage = "local", content = listOf("backup", "iso")),
                storage(storage = "pbs", content = listOf("vzdump")),
                storage(storage = "", content = listOf("backup"))
            ),
            backupsByStorage = mapOf(
                "local" to listOf(
                    backup(volid = "local:backup/vzdump-qemu-100.vma.zst", ctime = 10),
                    backup(volid = "", ctime = 30),
                    backup(volid = "local:iso/debian.iso", content = "iso"),
                    backup(volid = "local:backup/missing-format.vma.zst", format = null)
                ),
                "pbs" to listOf(
                    backup(volid = "pbs:backup/vzdump-qemu-101.vma.zst", ctime = 20),
                    backup(volid = "pbs:backup/negative-size.vma.zst", size = -1)
                )
            )
        )
        val repository = BackupRepository(api)

        val result = repository.getBackupInventory("pve")

        assertTrue(result is BackupResult.Success)
        val inventory = (result as BackupResult.Success).data
        assertEquals(listOf("local", "pbs"), inventory.storages.map { it.storage })
        assertEquals(
            listOf(
                "pbs:backup/vzdump-qemu-101.vma.zst",
                "local:backup/vzdump-qemu-100.vma.zst"
            ),
            inventory.backups.map { it.backup.volid }
        )
        assertEquals(listOf("local", "pbs"), api.contentRequests)
    }

    @Test
    fun getBackupInventory_returnsPartialStorageFailuresWithSuccessfulBackups() = runBlocking {
        val repository = BackupRepository(
            FakeBackupApi(
                storages = listOf(
                    storage(storage = "local"),
                    storage(storage = "backup-nfs")
                ),
                backupsByStorage = mapOf(
                    "local" to listOf(backup(volid = "local:backup/vzdump-qemu-100.vma.zst"))
                ),
                failingStorageNames = setOf("backup-nfs")
            )
        )

        val result = repository.getBackupInventory("pve")

        assertTrue(result is BackupResult.Success)
        val inventory = (result as BackupResult.Success).data
        assertEquals(listOf("local:backup/vzdump-qemu-100.vma.zst"), inventory.backups.map { it.backup.volid })
        assertEquals(listOf("backup-nfs"), inventory.storageErrors.map { it.storageName })
    }

    @Test
    fun getBackupInventory_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = BackupRepository(ProxmoxBackupApi { null })

        val result = repository.getBackupInventory("pve")

        assertTrue(result is BackupResult.Error)
        assertEquals("Not authenticated", (result as BackupResult.Error).message)
    }

    @Test
    fun getBackupInventory_reportsBlankNodeAsInvalidRequest() = runBlocking {
        val repository = BackupRepository(FakeBackupApi())

        val result = repository.getBackupInventory(" ")

        assertTrue(result is BackupResult.Error)
        assertEquals("Node name is required", (result as BackupResult.Error).message)
    }

    private class FakeBackupApi(
        private val storages: List<Storage> = listOf(storage()),
        private val backupsByStorage: Map<String, List<StorageContent>> = mapOf(
            "local" to listOf(backup())
        ),
        private val failingStorageNames: Set<String> = emptySet()
    ) : BackupApi {
        val contentRequests = mutableListOf<String>()

        override suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>> {
            return ApiResponse(storages)
        }

        override suspend fun getStorageContent(
            nodeName: String,
            storageName: String
        ): ApiResponse<List<StorageContent>> {
            contentRequests += storageName
            if (storageName in failingStorageNames) {
                throw RuntimeException("storage unavailable")
            }
            return ApiResponse(backupsByStorage[storageName].orEmpty())
        }
    }

    companion object {
        private fun storage(
            storage: String = "local",
            content: List<String> = listOf("backup")
        ): Storage {
            return Storage(
                storage = storage,
                type = "dir",
                content = content,
                nodes = null,
                shared = false,
                active = true,
                available = 10L * 1024L * 1024L * 1024L,
                used = 2L * 1024L * 1024L * 1024L,
                total = 12L * 1024L * 1024L * 1024L
            )
        }

        private fun backup(
            volid: String = "local:backup/vzdump-qemu-100.vma.zst",
            content: String = "backup",
            format: String? = "vma",
            size: Long = 1024L * 1024L,
            ctime: Long = 100
        ): StorageContent {
            return StorageContent(
                volid = volid,
                content = content,
                size = size,
                format = format,
                ctime = ctime,
                notes = null,
                vmid = null,
                used = null,
                parent = null,
                protectedContent = null
            )
        }
    }
}
