package com.example.neurotrack.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizedResourcesInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localizedResourcesPreservesActivityResultRegistryOwner() {
        composeRule.setContent {
            LocalizedResources("zh-CN") {
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                Box(Modifier.testTag("localized_content"))
            }
        }

        composeRule.onNodeWithTag("localized_content").assertIsDisplayed()
    }
}
