package com.proxmoxmobile.presentation.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proxmoxmobile.R
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.model.TaskLogEntry
import com.proxmoxmobile.data.task.TaskApi
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxmoxNavHostRecreationSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun postLoginNodeScopedTaskRouteSurvivesActivityRecreation() {
        assertRouteSurvivesActivityRecreation(
            route = Screen.NodeTasks.createRoute(POST_LOGIN_NODE),
            assertRouteVisible = ::assertNodeScopedTaskRouteVisible
        )
    }

    @Test
    fun postLoginResourceTaskRouteSurvivesActivityRecreation() {
        assertRouteSurvivesActivityRecreation(
            route = Screen.ResourceTasks.createRoute(POST_LOGIN_NODE, POST_LOGIN_VMID),
            assertRouteVisible = ::assertResourceTaskRouteVisible
        )
    }

    @Test
    fun postLoginTaskDetailRouteSurvivesActivityRecreation() {
        assertRouteSurvivesActivityRecreation(
            route = Screen.TaskDetail.createRoute(POST_LOGIN_NODE, POST_LOGIN_UPID),
            taskRepositoryOverride = TaskRepository(FakeTaskApi()),
            assertRouteVisible = ::assertTaskDetailRouteVisible
        )
    }

    @Test
    fun postLoginNodeNetworkRouteSurvivesActivityRecreation() {
        assertRouteSurvivesActivityRecreation(
            route = Screen.NodeNetwork.createRoute(POST_LOGIN_NODE),
            assertRouteVisible = ::assertNodeNetworkRouteVisible
        )
    }

    @Test
    fun postLoginStorageRouteSurvivesActivityRecreation() {
        assertRouteSurvivesActivityRecreation(
            route = Screen.Storage.createRoute(POST_LOGIN_NODE),
            assertRouteVisible = ::assertStorageRouteVisible
        )
    }

    private fun assertRouteSurvivesActivityRecreation(
        route: String,
        taskRepositoryOverride: TaskRepository? = null,
        assertRouteVisible: () -> Unit
    ) {
        lateinit var navController: NavHostController

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setProxmoxNavContent(taskRepositoryOverride) { navController = it }

            composeRule.runOnIdle {
                navController.navigate(route)
            }
            assertRouteVisible()

            scenario.recreate()
            scenario.setProxmoxNavContent(taskRepositoryOverride) { navController = it }

            assertRouteVisible()
        }
    }

    private fun ActivityScenario<ComponentActivity>.setProxmoxNavContent(
        taskRepositoryOverride: TaskRepository? = null,
        onNavControllerReady: (NavHostController) -> Unit
    ) {
        onActivity { activity ->
            activity.setContent {
                val navController = rememberNavController()
                onNavControllerReady(navController)

                ProxmoxTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ProxmoxNavHost(
                            navController = navController,
                            viewModel = fakeAuthenticatedViewModel(),
                            startDestination = Screen.Dashboard.route,
                            taskRepositoryOverride = taskRepositoryOverride
                        )
                    }
                }
            }
        }
    }

    private fun assertNodeScopedTaskRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.task_selected_node, POST_LOGIN_NODE))
            .assertIsDisplayed()
    }

    private fun assertResourceTaskRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.task_selected_node, POST_LOGIN_NODE))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(POST_LOGIN_VMID.toString())
            .assertIsDisplayed()
    }

    private fun assertTaskDetailRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_detail_title)).assertIsDisplayed()
        composeRule.onNodeWithText(POST_LOGIN_UPID).assertIsDisplayed()
        composeRule.onNodeWithText("fixture task completed").assertIsDisplayed()
    }

    private fun assertNodeNetworkRouteVisible() {
        composeRule.onNodeWithText(text(R.string.network_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.network_node_label, POST_LOGIN_NODE))
            .assertIsDisplayed()
    }

    private fun assertStorageRouteVisible() {
        composeRule
            .onNodeWithText(text(R.string.storage_title, POST_LOGIN_NODE))
            .assertIsDisplayed()
    }

    private fun fakeAuthenticatedViewModel(): MainViewModel {
        return MainViewModel().apply {
            setCurrentServer(
                ServerConfig(
                    host = "example.test",
                    port = 8006,
                    username = "tester",
                    password = null,
                    realm = "pam",
                    useHttps = true,
                    verifySsl = true
                )
            )
            setAuthenticated(true)
        }
    }

    private fun text(@StringRes id: Int): String {
        return targetContext.getString(id)
    }

    private fun text(@StringRes id: Int, vararg args: Any): String {
        return targetContext.getString(id, *args)
    }

    private class FakeTaskApi : TaskApi {
        override suspend fun getTasks(
            nodeName: String,
            limit: Int,
            start: Int,
            statusFilter: String?,
            typeFilter: String?,
            vmid: Int?
        ): ApiResponse<List<Task>> {
            return ApiResponse(listOf(fakeTask(nodeName = nodeName, upid = POST_LOGIN_UPID)))
        }

        override suspend fun getTaskStatus(nodeName: String, upid: String): ApiResponse<Task> {
            return ApiResponse(fakeTask(nodeName = nodeName, upid = upid))
        }

        override suspend fun getTaskLog(
            nodeName: String,
            upid: String,
            start: Int,
            limit: Int
        ): ApiResponse<List<TaskLogEntry>> {
            return ApiResponse(
                listOf(
                    TaskLogEntry(lineNumber = 1, text = "fixture task entered queue"),
                    TaskLogEntry(lineNumber = 2, text = "fixture task completed")
                )
            )
        }

        override suspend fun abortTask(nodeName: String, upid: String): ApiResponse<Map<String, String>> {
            return ApiResponse(mapOf("status" to "fixture-aborted"))
        }
    }

    private companion object {
        private const val POST_LOGIN_NODE = "lab-node"
        private const val POST_LOGIN_VMID = 102
        private const val POST_LOGIN_UPID = "UPID:fixture:0001:task:102:tester@pam:"

        private fun fakeTask(nodeName: String, upid: String): Task {
            return Task(
                upid = upid,
                id = POST_LOGIN_VMID.toString(),
                node = nodeName,
                pid = 4242,
                pstart = 1_700_000_000,
                type = "qmstart",
                status = "OK",
                exitstatus = "OK",
                starttime = 1_700_000_000,
                endtime = 1_700_000_030,
                user = "tester@pam",
                saved = true
            )
        }
    }
}
