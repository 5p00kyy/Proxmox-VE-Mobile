package com.proxmoxmobile.data.backup

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Backup
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.StorageContent
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class BackupRepository(
    private val api: BackupApi
) {
    suspend fun getBackupInventory(nodeName: String): BackupResult<BackupInventory> {
        return runBackupRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }

            val backupStorages = api.getStorages(nodeName)
                .data
                .filter { it.isValidBackupStorage() }
                .sortedBy { it.storage }

            val storageErrors = mutableListOf<BackupStorageError>()
            val backups = backupStorages
                .flatMap { storage ->
                    try {
                        api.getStorageContent(nodeName, storage.storage)
                            .data
                            .filter { it.isValidBackup() }
                            .map { storageContent ->
                                BackupEntry(
                                    storageName = storage.storage,
                                    backup = storageContent.toBackup()
                                )
                            }
                    } catch (e: Exception) {
                        storageErrors += BackupStorageError(
                            storageName = storage.storage,
                            message = e.toBackupErrorMessage()
                        )
                        emptyList()
                    }
                }
                .sortedWith(
                    compareByDescending<BackupEntry> { it.backup.ctime }
                        .thenBy { it.backup.volid }
                )

            BackupInventory(
                storages = backupStorages,
                backups = backups,
                storageErrors = storageErrors
            )
        }
    }

    private suspend fun <T> runBackupRequest(block: suspend () -> T): BackupResult<T> {
        return try {
            BackupResult.Success(block())
        } catch (e: Exception) {
            BackupResult.Error(e.toBackupErrorMessage())
        }
    }

    private fun Storage.isValidBackupStorage(): Boolean {
        return storage.isNotBlank() &&
            type.isNotBlank() &&
            content.any { it.equals("backup", ignoreCase = true) || it.equals("vzdump", ignoreCase = true) }
    }

    private fun StorageContent.isValidBackup(): Boolean {
        return volid.isNotBlank() &&
            content.equals("backup", ignoreCase = true) &&
            !format.isNullOrBlank() &&
            (size ?: -1) >= 0 &&
            (ctime ?: -1) >= 0
    }

    private fun StorageContent.toBackup(): Backup {
        return Backup(
            volid = volid,
            size = size ?: 0,
            format = format.orEmpty(),
            ctime = ctime ?: 0,
            content = content,
            notes = notes
        )
    }

    private fun Exception.toBackupErrorMessage(): String {
        return when (this) {
            is BackupNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Backup endpoint not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid backup request"
            else -> message ?: "Backup request failed"
        }
    }
}

interface BackupApi {
    suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>>

    suspend fun getStorageContent(
        nodeName: String,
        storageName: String
    ): ApiResponse<List<StorageContent>>
}

class ProxmoxBackupApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : BackupApi {
    override suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>> {
        return apiService().getStorages(nodeName)
    }

    override suspend fun getStorageContent(
        nodeName: String,
        storageName: String
    ): ApiResponse<List<StorageContent>> {
        return apiService().getStorageContent(nodeName, storageName)
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw BackupNotAuthenticatedException()
    }
}

sealed class BackupResult<out T> {
    data class Success<T>(val data: T) : BackupResult<T>()
    data class Error(val message: String) : BackupResult<Nothing>()
}

data class BackupInventory(
    val storages: List<Storage>,
    val backups: List<BackupEntry>,
    val storageErrors: List<BackupStorageError>
)

data class BackupEntry(
    val storageName: String,
    val backup: Backup
)

data class BackupStorageError(
    val storageName: String,
    val message: String
)

class BackupNotAuthenticatedException : IllegalStateException("Not authenticated")
