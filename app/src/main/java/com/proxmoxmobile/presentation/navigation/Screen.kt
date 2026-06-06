package com.proxmoxmobile.presentation.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ServerList : Screen("server_list")
    object Dashboard : Screen("dashboard")
    object VMList : Screen("vm_list") {
        fun createRoute(nodeName: String): String {
            return "vm_list/${nodeName.asRouteSegment()}"
        }
    }
    object ContainerList : Screen("container_list") {
        fun createRoute(nodeName: String): String {
            return "container_list/${nodeName.asRouteSegment()}"
        }
    }
    object Storage : Screen("storage") {
        fun createRoute(nodeName: String): String {
            return "storage/${nodeName.asRouteSegment()}"
        }
    }
    object Network : Screen("network")
    object NodeNetwork : Screen("network/{node}") {
        fun createRoute(node: String): String {
            return "network/${node.asRouteSegment()}"
        }
    }
    object Users : Screen("users")
    object Backups : Screen("backups")
    object Tasks : Screen("tasks")
    object NodeTasks : Screen("tasks/{node}") {
        fun createRoute(node: String): String {
            return "tasks/${node.asRouteSegment()}"
        }
    }
    object ResourceTasks : Screen("tasks/{node}/{vmid}") {
        fun createRoute(node: String, vmid: Int): String {
            return "tasks/${node.asRouteSegment()}/$vmid"
        }
    }
    object Cluster : Screen("cluster")
    object Settings : Screen("settings")
    
    // Detail screens
    object VMDetail : Screen("vm_detail/{vmid}") {
        fun createRoute(vmid: Int) = "vm_detail/$vmid"
    }

    object VMDetailWithNode : Screen("vm_detail/{node}/{vmid}") {
        fun createRoute(node: String, vmid: Int): String {
            return "vm_detail/${node.asRouteSegment()}/$vmid"
        }
    }
    
    object ContainerDetail : Screen("container_detail/{vmid}") {
        fun createRoute(vmid: Int) = "container_detail/$vmid"
    }

    object ContainerDetailWithNode : Screen("container_detail/{node}/{vmid}") {
        fun createRoute(node: String, vmid: Int): String {
            return "container_detail/${node.asRouteSegment()}/$vmid"
        }
    }
    
    object NodeDetail : Screen("node_detail/{node}") {
        fun createRoute(node: String): String {
            return "node_detail/${node.asRouteSegment()}"
        }
    }
    
    object StorageDetail : Screen("storage_detail/{storage}") {
        fun createRoute(storage: String) = "storage_detail/${storage.asRouteSegment()}"
    }
    
    object UserDetail : Screen("user_detail/{userid}") {
        fun createRoute(userid: String) = "user_detail/${userid.asRouteSegment()}"
    }
    
    object TaskDetail : Screen("task_detail/{node}/{upid}") {
        fun createRoute(node: String, upid: String): String {
            return "task_detail/${node.asRouteSegment()}/${upid.asRouteSegment()}"
        }
    }
}

internal fun String.asRouteSegment(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")
}
