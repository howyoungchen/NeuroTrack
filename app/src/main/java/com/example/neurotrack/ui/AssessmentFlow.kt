package com.example.neurotrack.ui

internal fun replaceAssessmentAnswer(
    answers: List<Int>,
    questionIndex: Int,
    optionIndex: Int,
): List<Int> {
    require(questionIndex in answers.indices) { "Question index is out of range" }
    require(optionIndex >= 0) { "Option index must be non-negative" }
    return answers.toMutableList().also { it[questionIndex] = optionIndex }
}

internal fun canAdvanceAssessment(answers: List<Int>, currentQuestion: Int): Boolean =
    currentQuestion in answers.indices && answers[currentQuestion] >= 0

internal fun canSubmitAssessment(answers: List<Int>): Boolean =
    answers.isNotEmpty() && answers.all { it >= 0 }
