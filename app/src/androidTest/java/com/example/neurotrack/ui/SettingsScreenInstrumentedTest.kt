package com.example.neurotrack.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.neurotrack.AppSettings
import com.example.neurotrack.ui.theme.NeuroTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsHomeOnlyShowsTheCurrentValueForEachChoice() {
        composeRule.setContent {
            NeuroTrackTheme {
                LocalizedResources("zh") {
                    SettingsScreen(
                        settings = AppSettings(),
                        onRefreshDay = {},
                        onLanguage = {},
                        onTheme = {},
                    )
                }
            }
        }

        assertEquals(
            "每周刷新日的七个候选值不应直接摊开在设置主页",
            1,
            visibleChoiceCount("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
        )
        assertEquals(
            "语言候选值不应直接摊开在设置主页",
            1,
            visibleChoiceCount("中文", "English"),
        )
        assertEquals(
            "外观候选值不应直接摊开在设置主页",
            1,
            visibleChoiceCount("跟随系统", "浅色", "深色"),
        )
    }

    @Test
    fun refreshDaySheetSelectsValueAndCloses() {
        var settings by mutableStateOf(AppSettings())
        composeRule.setContent {
            NeuroTrackTheme {
                LocalizedResources("zh") {
                    SettingsScreen(
                        settings = settings,
                        onRefreshDay = { day -> settings = settings.copy(refreshDay = day) },
                        onLanguage = {},
                        onTheme = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("每周刷新日").performClick()
        composeRule.onNodeWithText("选择每周刷新日").assertIsDisplayed()
        composeRule.onNodeWithText("周二").performClick()

        composeRule.onNodeWithText("选择每周刷新日").assertDoesNotExist()
        composeRule.onNodeWithText("周二").assertIsDisplayed()
    }

    private fun visibleChoiceCount(vararg labels: String): Int = labels.sumOf { label ->
        composeRule.onAllNodes(hasText(label, substring = false)).fetchSemanticsNodes().size
    }
}
