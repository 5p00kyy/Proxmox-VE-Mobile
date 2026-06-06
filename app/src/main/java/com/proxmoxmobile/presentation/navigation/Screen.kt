package com.proxmoxmobile.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ServerList : Screen("server_list")
    object Dashboard : Screen("dashboard")
    object VMList : Screen("vm_list")
    object ContainerList : Screen("container_list")
    object Storage : Screen("storage")
    object Network : Screen("network")
    object NodeNetwork : Screen("network/{node}") {
        fun createRoute(node: String): String {
            return "network/${Uri.encode(node)}"
        }
    }
    object Users : Screen("users")
    object Backups : Screen("backups")
    object Tasks : Screen("tasks")
    object NodeTasks : Screen("tasks/{node}") {
        fun createRoute(node: String): String {
            return "tasks/${Uri.encode(node)}"
        }
    }
    object ResourceTasks : Screen("tasks/{node}/{vmid}") {
        fun createRoute(node: String, vmid: Int): String {
            return "tasks/${Uri.encode(node)}/$vmid"
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
            return "vm_detail/${Uri.encode(node)}/$vmid"
        }
    }
    
    object ContainerDetail : Screen("container_detail/{vmid}") {
        fun createRoute(vmid: Int) = "container_detail/$vmid"
    }

    object ContainerDetailWithNode : Screen("container_detail/{node}/{vmid}") {
        fun createRoute(node: String, vmid: Int): String {
            return "container_detail/${Uri.encode(node)}/$vmid"
        }
    }
    
    object NodeDetail : Screen("node_detail/{node}") {
        fun createRoute(node: String): String {
            return "node_detail/${Uri.encode(node)}"
        }
    }
    
    object StorageDetail : Screen("storage_detail/{storage}") {
        fun createRoute(storage: String) = "storage_detail/$storage"
    }
    
    object UserDetail : Screen("user_detail/{userid}") {
        fun createRoute(userid: String) = "user_detail/$userid"
    }
    
    object TaskDetail : Screen("task_detail/{node}/{upid}") {
        fun createRoute(node: String, upid: String): String {
            return "task_detail/${Uri.encode(node)}/${Uri.encode(upid)}"
        }
    }
}
