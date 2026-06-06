package com.proxmoxmobile.data.cluster

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.ClusterStatusEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterRepositoryTest {
    @Test
    fun getClusterOverview_mapsClusterAndNodeRows() = runBlocking {
        val repository = ClusterRepository(
            FakeClusterApi(
                entries = listOf(
                    clusterEntry(name = "lab", quorate = 1, nodes = 2, votes = 2, expectedVotes = 2),
                    nodeEntry(name = "pve-b", nodeId = 2, ip = "10.0.0.12", online = 0),
                    nodeEntry(name = "pve-a", nodeId = 1, ip = "10.0.0.11", online = 1, local = 1),
                    nodeEntry(name = "", nodeId = 3, ip = "10.0.0.13", online = 1)
                )
            )
        )

        val result = repository.getClusterOverview()

        assertTrue(result is ClusterResult.Success)
        val overview = (result as ClusterResult.Success).data
        assertEquals("lab", overview.clusterName)
        assertTrue(overview.quorate)
        assertEquals(2, overview.nodeCount)
        assertEquals(2, overview.votes)
        assertEquals(2, overview.expectedVotes)
        assertEquals(listOf("pve-a", "pve-b"), overview.nodes.map { it.name })
        assertTrue(overview.nodes.first().online)
        assertTrue(overview.nodes.first().local)
        assertFalse(overview.nodes.last().online)
    }

    @Test
    fun getClusterOverview_handlesStandaloneNodeRows() = runBlocking {
        val repository = ClusterRepository(
            FakeClusterApi(
                entries = listOf(
                    nodeEntry(name = "pve", nodeId = 1, ip = "10.0.0.10", online = 1)
                )
            )
        )

        val result = repository.getClusterOverview()

        assertTrue(result is ClusterResult.Success)
        val overview = (result as ClusterResult.Success).data
        assertEquals("", overview.clusterName)
        assertFalse(overview.quorate)
        assertEquals(1, overview.nodeCount)
        assertEquals(listOf("pve"), overview.nodes.map { it.name })
    }

    @Test
    fun getClusterOverview_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = ClusterRepository(ProxmoxClusterApi { null })

        val result = repository.getClusterOverview()

        assertTrue(result is ClusterResult.Error)
        assertEquals("Not authenticated", (result as ClusterResult.Error).message)
    }

    private class FakeClusterApi(
        private val entries: List<ClusterStatusEntry>
    ) : ClusterApi {
        override suspend fun getClusterStatus(): ApiResponse<List<ClusterStatusEntry>> {
            return ApiResponse(entries)
        }
    }

    companion object {
        private fun clusterEntry(
            name: String,
            quorate: Int,
            nodes: Int,
            votes: Int,
            expectedVotes: Int
        ): ClusterStatusEntry {
            return ClusterStatusEntry(
                type = "cluster",
                name = name,
                nodeid = null,
                ip = null,
                local = null,
                online = null,
                level = null,
                quorate = quorate,
                nodes = nodes,
                votes = votes,
                expected_votes = expectedVotes
            )
        }

        private fun nodeEntry(
            name: String,
            nodeId: Int,
            ip: String,
            online: Int,
            local: Int = 0
        ): ClusterStatusEntry {
            return ClusterStatusEntry(
                type = "node",
                name = name,
                nodeid = nodeId,
                ip = ip,
                local = local,
                online = online,
                level = "",
                quorate = null,
                nodes = null,
                votes = null,
                expected_votes = null
            )
        }
    }
}
