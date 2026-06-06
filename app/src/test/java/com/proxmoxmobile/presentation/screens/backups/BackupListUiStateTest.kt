package com.proxmoxmobile.presentation.screens.backups

import com.proxmoxmobile.data.backup.BackupEntry
import com.proxmoxmobile.data.model.Backup
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupListUiStateTest {
    @Test
    fun visibleBackupEntries_returnsAllBackupsWhenNoStorageFilterIsSelected() {
        val backups = listOf(
            backupEntry(storageName = "local", volid = "local:backup/vzdump-qemu-100.vma.zst"),
            backupEntry(storageName = "pbs", volid = "pbs:backup/vzdump-lxc-101.tar.zst")
        )
        val state = BackupListUiState(
            selectedStorageName = null,
            backupEntries = backups
        )

        assertEquals(backups, state.visibleBackupEntries)
    }

    @Test
    fun visibleBackupEntries_filtersBackupsBySelectedStorage() {
        val state = BackupListUiState(
            selectedStorageName = "pbs",
            backupEntries = listOf(
                backupEntry(storageName = "local", volid = "local:backup/vzdump-qemu-100.vma.zst"),
                backupEntry(storageName = "pbs", volid = "pbs:backup/vzdump-lxc-101.tar.zst"),
                backupEntry(storageName = "pbs", volid = "pbs:backup/vzdump-qemu-102.vma.zst")
            )
        )

        assertEquals(
            listOf(
                "pbs:backup/vzdump-lxc-101.tar.zst",
                "pbs:backup/vzdump-qemu-102.vma.zst"
            ),
            state.visibleBackupEntries.map { it.backup.volid }
        )
    }

    private fun backupEntry(
        storageName: String,
        volid: String
    ): BackupEntry {
        return BackupEntry(
            storageName = storageName,
            backup = Backup(
                volid = volid,
                size = 1024L,
                format = "vma",
                ctime = 100L,
                content = "backup",
                notes = null
            )
        )
    }
}
