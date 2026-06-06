package com.proxmoxmobile.presentation.screens.tasks

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskFilterDraftPersistenceSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun unsavedTaskFilterDraftSurvivesActivityRecreation() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setTaskContent()

            composeRule.onNodeWithTag(TASK_FILTER_TYPE_TAG)
                .performScrollTo()
                .performTextReplacement(TYPE_DRAFT)
            composeRule.onNodeWithTag(TASK_FILTER_VMID_TAG)
                .performScrollTo()
                .performTextReplacement(VMID_DRAFT)

            scenario.recreate()
            scenario.setTaskContent()

            composeRule.onNodeWithTag(TASK_FILTER_TYPE_TAG)
                .performScrollTo()
                .assertTextContains(TYPE_DRAFT)
            composeRule.onNodeWithTag(TASK_FILTER_VMID_TAG)
                .performScrollTo()
                .assertTextContains(VMID_DRAFT)
        }
    }

    private fun ActivityScenario<ComponentActivity>.setTaskContent() {
        onActivity { activity ->
            activity.setContent {
                val navController = rememberNavController()

                ProxmoxTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        TaskScreen(
                            navController = navController,
                            viewModel = MainViewModel()
                        )
                    }
                }
            }
        }
    }

    private companion object {
        private const val TYPE_DRAFT = "qmstart"
        private const val VMID_DRAFT = "102"
    }
}
