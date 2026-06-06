package com.proxmoxmobile.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {
    @Test
    fun betaRouteRegistry_containsRegisteredNavHostRoutes() {
        val registeredRoutes = setOf(
            Screen.Login.route,
            Screen.ServerList.route,
            Screen.Dashboard.route,
            Screen.VMList.pattern,
            Screen.ContainerList.pattern,
            Screen.Storage.pattern,
            Screen.Network.route,
            Screen.NodeNetwork.route,
            Screen.Users.route,
            Screen.Backups.route,
            Screen.Tasks.route,
            Screen.NodeTasks.route,
            Screen.ResourceTasks.route,
            Screen.Cluster.route,
            Screen.Settings.route,
            Screen.VMDetail.route,
            Screen.VMDetailWithNode.route,
            Screen.ContainerDetail.route,
            Screen.ContainerDetailWithNode.route,
            Screen.NodeDetail.route,
            Screen.TaskDetail.route
        )

        assertEquals(registeredRoutes, Screen.betaRegisteredRoutes)
    }

    @Test
    fun plannedRoutes_areKnownButExcludedFromBetaRegistry() {
        assertEquals(
            setOf(Screen.StorageDetail.route, Screen.UserDetail.route),
            Screen.plannedRoutes
        )
        assertTrue(Screen.allKnownRoutes.containsAll(Screen.plannedRoutes))
        assertFalse(Screen.betaRegisteredRoutes.contains(Screen.StorageDetail.route))
        assertFalse(Screen.betaRegisteredRoutes.contains(Screen.UserDetail.route))
    }

    @Test
    fun knownRoutePatterns_areUniqueAcrossRegisteredAndPlannedRoutes() {
        val routes = Screen.allKnownRoutePatterns

        assertEquals(routes.distinct(), routes)
    }

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
        val upid = "UPID:fixture:0030B37E:070069EE:task:102:tester@pam:"

        assertEquals("storage_detail/local-lvm%2Fvm-102", Screen.StorageDetail.createRoute("local-lvm/vm-102"))
        assertEquals("user_detail/tester%40pam", Screen.UserDetail.createRoute("tester@pam"))
        assertEquals(
            "task_detail/pve%2Fnode/UPID%3Afixture%3A0030B37E%3A070069EE%3Atask%3A102%3Atester%40pam%3A",
            Screen.TaskDetail.createRoute("pve/node", upid)
        )
    }

    @Test
    fun taskDetailRouteForNotice_returnsEncodedRouteWhenNodeAndTaskExist() {
        assertEquals(
            "task_detail/lab%20node%2F1/UPID%3Afixture%3Atask%3Atester%40pam%3A",
            taskDetailRouteForNotice(" lab node/1 ", " UPID:fixture:task:tester@pam: ")
        )
    }

    @Test
    fun taskDetailRouteForNotice_returnsNullForMissingNodeOrTask() {
        assertEquals(null, taskDetailRouteForNotice(null, "UPID:fixture:task"))
        assertEquals(null, taskDetailRouteForNotice("   ", "UPID:fixture:task"))
        assertEquals(null, taskDetailRouteForNotice("pve", null))
        assertEquals(null, taskDetailRouteForNotice("pve", "   "))
    }
}
