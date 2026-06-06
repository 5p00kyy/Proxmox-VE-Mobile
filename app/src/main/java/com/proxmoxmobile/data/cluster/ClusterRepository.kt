package com.proxmoxmobile.data.cluster

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.ClusterStatusEntry
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ClusterRepository(
    private val api: ClusterApi
) {
    suspend fun getClusterOverview(): ClusterResult<ClusterOverview> {
        return runClusterRequest {
            val entries = api.getClusterStatus().data
            val clusterEntry = entries.firstOrNull { it.type == "cluster" }
            val nodeEntries = entries
                .filter { it.type == "node" && !it.name.isNullOrBlank() }
                .map {
                    ClusterNodeSummary(
                        name = it.name.orEmpty(),
                        nodeId = it.nodeid,
                        ip = it.ip.orEmpty(),
                        online = it.online == 1,
                        local = it.local == 1,
                        level = it.level.orEmpty()
                    )
                }
                .sortedWith(compareByDescending<ClusterNodeSummary> { it.online }.thenBy { it.name })

            ClusterOverview(
                clusterName = clusterEntry?.name.orEmpty(),
                quorate = clusterEntry?.quorate == 1,
                nodeCount = clusterEntry?.nodes ?: nodeEntries.size,
                expectedVotes = clusterEntry?.expected_votes ?: 0,
                votes = clusterEntry?.votes ?: 0,
                nodes = nodeEntries
            )
        }
    }

    private suspend fun <T> runClusterRequest(block: suspend () -> T): ClusterResult<T> {
        return try {
            ClusterResult.Success(block())
        } catch (e: Exception) {
            ClusterResult.Error(e.toClusterErrorMessage())
        }
    }

    private fun Exception.toClusterErrorMessage(): String {
        return when (this) {
            is ClusterNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            else -> message ?: "Cluster request failed"
        }
    }
}

interface ClusterApi {
    suspend fun getClusterStatus(): ApiResponse<List<ClusterStatusEntry>>
}

class ProxmoxClusterApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : ClusterApi {
    override suspend fun getClusterStatus(): ApiResponse<List<ClusterStatusEntry>> {
        return apiService().getClusterStatus()
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw ClusterNotAuthenticatedException()
    }
}

data class ClusterOverview(
    val clusterName: String,
    val quorate: Boolean,
    val nodeCount: Int,
    val expectedVotes: Int,
    val votes: Int,
    val nodes: List<ClusterNodeSummary>
)

data class ClusterNodeSummary(
    val name: String,
    val nodeId: Int?,
    val ip: String,
    val online: Boolean,
    val local: Boolean,
    val level: String
)

sealed class ClusterResult<out T> {
    data class Success<T>(val data: T) : ClusterResult<T>()
    data class Error(val message: String) : ClusterResult<Nothing>()
}

class ClusterNotAuthenticatedException : IllegalStateException("Not authenticated")
