package com.proxmoxmobile.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTest {
    @Test
    fun nodeScopedRoutes_encodeNodePathSegments() {
        val node = "lab node/1"

        assertEquals("vm_list/lab%20node%2F1", Screen.VMList.createRoute(node))
        assertEquals("container_list/lab%20node%2F1", Screen.ContainerList.createRoute(node))
        assertEquals("storage/lab%20node%2F1", Screen.Storage.createRoute(node))
        assertEquals("network/lab%20node%2F1", Screen.NodeNetwork.createRoute(node))
        assertEquals("tasks/lab%20node%2F1", Screen.NodeTasks.createRoute(node))
        assertEquals("node_detail/lab%20node%2F1", Screen.NodeDetail.createRoute(node))
    }

    @Test
    fun resourceRoutes_encodePathSegmentsWithoutEncodingIds() {
        val node = "cluster/a"

        assertEquals("tasks/cluster%2Fa/102", Screen.ResourceTasks.createRoute(node, 102))
        assertEquals("vm_detail/cluster%2Fa/102", Screen.VMDetailWithNode.createRoute(node, 102))
        assertEquals(
            "container_detail/cluster%2Fa/102",
            Screen.ContainerDetailWithNode.createRoute(node, 102)
        )
    }

    @Test
    fun detailRoutes_encodeStorageUserAndTaskIdentifiers() {
        val upid = "UPID:pve:0030B37E:070069EE:task:102:root@pam:"

        assertEquals("storage_detail/local-lvm%2Fvm-102", Screen.StorageDetail.createRoute("local-lvm/vm-102"))
        assertEquals("user_detail/root%40pam", Screen.UserDetail.createRoute("root@pam"))
        assertEquals(
            "task_detail/pve%2Fnode/UPID%3Apve%3A0030B37E%3A070069EE%3Atask%3A102%3Aroot%40pam%3A",
            Screen.TaskDetail.createRoute("pve/node", upid)
        )
    }
}
