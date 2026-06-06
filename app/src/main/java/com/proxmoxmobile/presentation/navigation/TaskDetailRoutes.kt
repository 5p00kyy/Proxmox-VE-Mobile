package com.proxmoxmobile.presentation.navigation

fun taskDetailRouteForNotice(
    nodeName: String?,
    taskId: String?
): String? {
    val node = nodeName?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val upid = taskId?.trim()?.takeIf { it.isNotBlank() } ?: return null

    return Screen.TaskDetail.createRoute(node, upid)
}
