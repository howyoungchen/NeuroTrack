package com.example.neurotrack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.neurotrack.ui.theme.NeuroTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindfulnessRoundCardInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun courseListStaysBehindRoundEntryAndOpensLessonDetails() {
        composeRule.setContent {
            NeuroTrackTheme {
                LocalizedResources("zh") {
                    MindfulnessRoundCard(
                        completedLessonIds = emptySet(),
                        refreshDayLabel = "周一",
                        onStartMindfulness = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("感官觉察落地练习").assertDoesNotExist()
        composeRule.onNodeWithText("查看本轮课程").performClick()
        composeRule.onNodeWithText("本轮课程").assertIsDisplayed()
        composeRule.onNodeWithText("感官觉察落地练习").performClick()
        composeRule.onNodeWithText("开始本节").assertIsDisplayed()
    }
}
