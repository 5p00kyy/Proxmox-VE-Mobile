package com.proxmoxmobile.data.dashboard

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Node
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.task.TaskResult
import com.proxmoxmobile.data.task.TaskSummary
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class DashboardRepository(
    private val api: DashboardApi,
    private val taskSummarySource: DashboardTaskSummarySource
) {
    suspend fun getDashboardSnapshot(): DashboardResult<DashboardSnapshot> {
        return runDashboardRequest {
            val nodes = api.getNodes()
                .data
                .filter { it.isValid() }
                .sortedBy { it.node }

            val taskSummary = if (nodes.isEmpty()) {
                DashboardTaskSummaryState()
            } else {
                when (val result = taskSummarySource.getTaskSummary(nodes.map { it.node })) {
                    is TaskResult.Success -> DashboardTaskSummaryState(summary = result.data)
                    is TaskResult.Error -> DashboardTaskSummaryState(errorMessage = result.message)
                }
            }

            DashboardSnapshot(
                nodes = nodes,
                taskSummary = taskSummary
            )
        }
    }

    private suspend fun <T> runDashboardRequest(block: suspend () -> T): DashboardResult<T> {
        return try {
            DashboardResult.Success(block())
        } catch (e: Exception) {
            DashboardResult.Error(e.toDashboardErrorMessage())
        }
    }

    private fun Node.isValid(): Boolean {
        return runCatching {
            node.isNotBlank() &&
                status.isNotBlank() &&
                cpu >= 0 &&
                mem >= 0 &&
                maxmem >= 0 &&
                uptime >= 0
        }.getOrDefault(false)
    }

    private fun Exception.toDashboardErrorMessage(): String {
        return when (this) {
            is DashboardNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Node endpoint not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid dashboard request"
            else -> message ?: "Dashboard request failed"
        }
    }
}

interface DashboardApi {
    suspend fun getNodes(): ApiResponse<List<Node>>
}

class ProxmoxDashboardApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : DashboardApi {
    override suspend fun getNodes(): ApiResponse<List<Node>> {
        return apiService().getNodes()
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw DashboardNotAuthenticatedException()
    }
}

interface DashboardTaskSummarySource {
    suspend fun getTaskSummary(nodeNames: List<String>): TaskResult<TaskSummary>
}

class ProxmoxDashboardTaskSummarySource(
    private val taskRepository: TaskRepository
) : DashboardTaskSummarySource {
    override suspend fun getTaskSummary(nodeNames: List<String>): TaskResult<TaskSummary> {
        return taskRepository.getTaskSummary(nodeNames)
    }
}

sealed class DashboardResult<out T> {
    data class Success<T>(val data: T) : DashboardResult<T>()
    data class Error(val message: String) : DashboardResult<Nothing>()
}

data class DashboardSnapshot(
    val nodes: List<Node>,
    val taskSummary: DashboardTaskSummaryState
)

data class DashboardTaskSummaryState(
    val summary: TaskSummary? = null,
    val errorMessage: String? = null
)

class DashboardNotAuthenticatedException : IllegalStateException("Not authenticated")
