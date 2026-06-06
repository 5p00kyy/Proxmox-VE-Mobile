package com.proxmoxmobile.data.task

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.model.TaskLogEntry
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class TaskRepository(
    private val api: TaskApi
) {
    suspend fun getTasks(
        nodeName: String,
        limit: Int = 100,
        filters: TaskFilters = TaskFilters()
    ): TaskResult<List<Task>> {
        return runTaskRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(limit > 0) { "Task limit must be greater than zero" }
            filters.vmid?.let { require(it > 0) { "VMID filter must be greater than zero" } }

            api.getTasks(
                nodeName = nodeName,
                limit = limit,
                statusFilter = filters.status.apiValue,
                typeFilter = filters.normalizedTypeFilter(),
                vmid = filters.vmid
            )
                .data
                .filter { it.isValidForTaskOperations() }
                .sortedByDescending { it.starttime }
        }
    }

    suspend fun getTaskSummary(
        nodeNames: List<String>,
        limitPerNode: Int = 25
    ): TaskResult<TaskSummary> {
        return runTaskRequest {
            val distinctNodes = nodeNames
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            require(distinctNodes.isNotEmpty()) { "At least one node is required" }
            require(limitPerNode > 0) { "Task limit must be greater than zero" }

            val tasks = distinctNodes
                .flatMap { nodeName ->
                    api.getTasks(nodeName, limit = limitPerNode)
                        .data
                        .filter { it.isValidForTaskOperations() }
                }
                .sortedByDescending { it.starttime }

            TaskSummary(
                nodesChecked = distinctNodes.size,
                runningCount = tasks.count { it.status.equals("running", ignoreCase = true) },
                recentCount = tasks.size,
                latestTask = tasks.firstOrNull()
            )
        }
    }

    suspend fun getTaskDetail(nodeName: String, upid: String): TaskResult<TaskDetail> {
        return runTaskRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(upid.isNotBlank()) { "Task UPID is required" }

            val status = api.getTaskStatus(nodeName, upid).data
            val logEntries = api.getTaskLog(nodeName, upid)
                .data
                .filter { it.text.isNotBlank() }
                .sortedBy { it.lineNumber }

            TaskDetail(
                nodeName = nodeName,
                upid = upid,
                task = status,
                logEntries = logEntries
            )
        }
    }

    suspend fun abortTask(nodeName: String, upid: String): TaskResult<Unit> {
        return runTaskRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(upid.isNotBlank()) { "Task UPID is required" }
            api.abortTask(nodeName, upid)
            Unit
        }
    }

    private suspend fun <T> runTaskRequest(block: suspend () -> T): TaskResult<T> {
        return try {
            TaskResult.Success(block())
        } catch (e: Exception) {
            TaskResult.Error(e.toTaskErrorMessage())
        }
    }

    private fun Task.isValidForTaskOperations(): Boolean {
        return taskUpid() != null &&
            node.isNotBlank() &&
            type.isNotBlank() &&
            status.isNotBlank() &&
            starttime >= 0
    }

    private fun Exception.toTaskErrorMessage(): String {
        return when (this) {
            is TaskNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Task not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid task request"
            else -> message ?: "Task request failed"
        }
    }
}

interface TaskApi {
    suspend fun getTasks(
        nodeName: String,
        limit: Int = 100,
        start: Int = 0,
        statusFilter: String? = null,
        typeFilter: String? = null,
        vmid: Int? = null
    ): ApiResponse<List<Task>>

    suspend fun getTaskStatus(
        nodeName: String,
        upid: String
    ): ApiResponse<Task>

    suspend fun getTaskLog(
        nodeName: String,
        upid: String,
        start: Int = 0,
        limit: Int = 500
    ): ApiResponse<List<TaskLogEntry>>

    suspend fun abortTask(
        nodeName: String,
        upid: String
    ): ApiResponse<Map<String, String>>
}

class ProxmoxTaskApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : TaskApi {
    override suspend fun getTasks(
        nodeName: String,
        limit: Int,
        start: Int,
        statusFilter: String?,
        typeFilter: String?,
        vmid: Int?
    ): ApiResponse<List<Task>> {
        return apiService().getTasks(
            node = nodeName,
            limit = limit,
            start = start,
            statusFilter = statusFilter,
            typeFilter = typeFilter,
            vmid = vmid
        )
    }

    override suspend fun getTaskStatus(nodeName: String, upid: String): ApiResponse<Task> {
        return apiService().getTaskStatus(nodeName, upid)
    }

    override suspend fun getTaskLog(
        nodeName: String,
        upid: String,
        start: Int,
        limit: Int
    ): ApiResponse<List<TaskLogEntry>> {
        return apiService().getTaskLog(nodeName, upid, start = start, limit = limit)
    }

    override suspend fun abortTask(
        nodeName: String,
        upid: String
    ): ApiResponse<Map<String, String>> {
        return apiService().deleteTask(nodeName, upid)
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw TaskNotAuthenticatedException()
    }
}

sealed class TaskResult<out T> {
    data class Success<T>(val data: T) : TaskResult<T>()
    data class Error(val message: String) : TaskResult<Nothing>()
}

data class TaskDetail(
    val nodeName: String,
    val upid: String,
    val task: Task,
    val logEntries: List<TaskLogEntry>
)

data class TaskSummary(
    val nodesChecked: Int,
    val runningCount: Int,
    val recentCount: Int,
    val latestTask: Task?
)

data class TaskFilters(
    val status: TaskStatusFilter = TaskStatusFilter.All,
    val typeFilter: String? = null,
    val vmid: Int? = null
) {
    fun hasActiveFilters(): Boolean {
        return status != TaskStatusFilter.All ||
            !typeFilter.isNullOrBlank() ||
            vmid != null
    }

    fun normalizedTypeFilter(): String? {
        return typeFilter?.trim()?.takeIf { it.isNotBlank() }
    }
}

enum class TaskStatusFilter(val apiValue: String?) {
    All(null),
    Running("running"),
    Finished("finished"),
    Stopped("stopped")
}

fun Task.taskUpid(): String? {
    return upid?.takeIf { it.isNotBlank() }
        ?: id.takeIf { it.startsWith("UPID:") }
}

class TaskNotAuthenticatedException : IllegalStateException("Not authenticated")
