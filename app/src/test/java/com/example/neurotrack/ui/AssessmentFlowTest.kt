package com.example.neurotrack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentFlowTest {
    @Test
    fun replaceAssessmentAnswer_updatesOnlyTheCurrentQuestion() {
        val answers = listOf(-1, -1, 2)

        assertEquals(
            listOf(-1, 1, 2),
            replaceAssessmentAnswer(answers, questionIndex = 1, optionIndex = 1),
        )
        assertEquals(listOf(-1, -1, 2), answers)
    }

    @Test
    fun canAdvanceAssessment_requiresAnAnswerForTheVisibleQuestion() {
        val answers = listOf(0, -1, 2)

        assertTrue(canAdvanceAssessment(answers, currentQuestion = 0))
        assertFalse(canAdvanceAssessment(answers, currentQuestion = 1))
    }

    @Test
    fun canSubmitAssessment_requiresEveryQuestionToBeAnswered() {
        assertTrue(canSubmitAssessment(listOf(0, 1, 2, 3)))
        assertFalse(canSubmitAssessment(listOf(0, -1, 2, 3)))
        assertFalse(canSubmitAssessment(emptyList()))
    }
}
