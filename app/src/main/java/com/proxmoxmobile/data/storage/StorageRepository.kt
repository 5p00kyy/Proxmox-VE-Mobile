package com.proxmoxmobile.data.storage

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.StorageContent
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class StorageRepository(
    private val api: StorageApi
) {
    suspend fun getStorages(nodeName: String): StorageResult<List<Storage>> {
        return runStorageRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            api.getStorages(nodeName)
                .data
                .filter { it.isValid() }
                .sortedBy { it.storage }
        }
    }

    suspend fun getStorageContent(
        nodeName: String,
        storageName: String
    ): StorageResult<List<StorageContent>> {
        return runStorageRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(storageName.isNotBlank()) { "Storage name is required" }

            api.getStorageContent(nodeName, storageName)
                .data
                .filter { it.isValid() }
                .sortedWith(compareBy<StorageContent> { it.content }.thenBy { it.volid })
        }
    }

    private suspend fun <T> runStorageRequest(block: suspend () -> T): StorageResult<T> {
        return try {
            StorageResult.Success(block())
        } catch (e: Exception) {
            StorageResult.Error(e.toStorageErrorMessage())
        }
    }

    private fun Storage.isValid(): Boolean {
        return storage.isNotBlank() &&
            type.isNotBlank() &&
            available >= 0 &&
            used >= 0 &&
            total >= 0
    }

    private fun StorageContent.isValid(): Boolean {
        return volid.isNotBlank() &&
            content.isNotBlank() &&
            (size == null || size >= 0) &&
            (ctime == null || ctime >= 0) &&
            (used == null || used >= 0)
    }

    private fun Exception.toStorageErrorMessage(): String {
        return when (this) {
            is StorageNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Node or storage not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid storage request"
            else -> message ?: "Storage request failed"
        }
    }
}

interface StorageApi {
    suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>>

    suspend fun getStorageContent(
        nodeName: String,
        storageName: String
    ): ApiResponse<List<StorageContent>>
}

class ProxmoxStorageApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : StorageApi {
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
        return apiServiceProvider() ?: throw StorageNotAuthenticatedException()
    }
}

sealed class StorageResult<out T> {
    data class Success<T>(val data: T) : StorageResult<T>()
    data class Error(val message: String) : StorageResult<Nothing>()
}

class StorageNotAuthenticatedException : IllegalStateException("Not authenticated")
