package com.example.neurotrack.mindfulness

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.example.neurotrack.R

data class MindfulnessLesson(
    val id: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val durationRes: Int,
    @param:RawRes val audioRes: Int,
    val estimatedDurationMinutes: Int,
)

object MindfulnessLessons {
    val all: List<MindfulnessLesson> = listOf(
        MindfulnessLesson(
            id = 1,
            titleRes = R.string.mindfulness_lesson_1_title,
            descriptionRes = R.string.mindfulness_lesson_1_desc,
            durationRes = R.string.mindfulness_lesson_1_duration,
            audioRes = R.raw.mindfulness_01,
            estimatedDurationMinutes = 7,
        ),
        MindfulnessLesson(
            id = 2,
            titleRes = R.string.mindfulness_lesson_2_title,
            descriptionRes = R.string.mindfulness_lesson_2_desc,
            durationRes = R.string.mindfulness_lesson_2_duration,
            audioRes = R.raw.mindfulness_02,
            estimatedDurationMinutes = 9,
        ),
        MindfulnessLesson(
            id = 3,
            titleRes = R.string.mindfulness_lesson_3_title,
            descriptionRes = R.string.mindfulness_lesson_3_desc,
            durationRes = R.string.mindfulness_lesson_3_duration,
            audioRes = R.raw.mindfulness_03,
            estimatedDurationMinutes = 14,
        ),
        MindfulnessLesson(
            id = 4,
            titleRes = R.string.mindfulness_lesson_4_title,
            descriptionRes = R.string.mindfulness_lesson_4_desc,
            durationRes = R.string.mindfulness_lesson_4_duration,
            audioRes = R.raw.mindfulness_04,
            estimatedDurationMinutes = 11,
        ),
        MindfulnessLesson(
            id = 5,
            titleRes = R.string.mindfulness_lesson_5_title,
            descriptionRes = R.string.mindfulness_lesson_5_desc,
            durationRes = R.string.mindfulness_lesson_5_duration,
            audioRes = R.raw.mindfulness_05,
            estimatedDurationMinutes = 11,
        ),
        MindfulnessLesson(
            id = 6,
            titleRes = R.string.mindfulness_lesson_6_title,
            descriptionRes = R.string.mindfulness_lesson_6_desc,
            durationRes = R.string.mindfulness_lesson_6_duration,
            audioRes = R.raw.mindfulness_06,
            estimatedDurationMinutes = 16,
        ),
    )

    fun find(id: Int): MindfulnessLesson? = all.firstOrNull { it.id == id }
}
