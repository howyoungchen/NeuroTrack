package com.example.neurotrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neurotrack.R
import com.example.neurotrack.domain.MindfulnessSchedule
import com.example.neurotrack.mindfulness.MindfulnessLesson
import com.example.neurotrack.mindfulness.MindfulnessLessons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MindfulnessRoundCard(
    completedLessonIds: Set<Int>,
    refreshDayLabel: String,
    onStartMindfulness: (Int) -> Unit,
) {
    var coursesOpen by rememberSaveable { mutableStateOf(false) }
    var selectedLessonId by rememberSaveable { mutableStateOf<Int?>(null) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Headphones, contentDescription = null, modifier = Modifier.size(21.dp))
                    }
                }
                Text(
                    stringResource(R.string.practice_weekly_rhythm),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                stringResource(R.string.practice_completed_format, completedLessonIds.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.practice_schedule, refreshDayLabel),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(completedLessonIds.size / MindfulnessSchedule.LESSON_COUNT.toFloat())
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondary, CircleShape),
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MindfulnessSchedule.lessonIds.forEach { lessonId ->
                    val completed = lessonId in completedLessonIds
                    Surface(
                        shape = CircleShape,
                        color = if (completed) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (completed) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            } else {
                                Text(lessonId.toString(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            if (completedLessonIds.size == MindfulnessSchedule.LESSON_COUNT) {
                Text(
                    stringResource(R.string.practice_round_complete),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedLessonId = null
                        coursesOpen = true
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(Icons.Rounded.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.mindfulness_courses_entry),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (coursesOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                coursesOpen = false
                selectedLessonId = null
            },
            sheetState = sheetState,
        ) {
            Box(Modifier.fillMaxWidth()) {
                val selectedLesson = selectedLessonId?.let(MindfulnessLessons::find)
                if (selectedLesson == null) {
                    MindfulnessCourseList(
                        completedLessonIds = completedLessonIds,
                        onSelectLesson = { selectedLessonId = it.id },
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .align(Alignment.TopCenter),
                    )
                } else {
                    MindfulnessLessonDetail(
                        lesson = selectedLesson,
                        completed = selectedLesson.id in completedLessonIds,
                        onBack = { selectedLessonId = null },
                        onStart = {
                            coursesOpen = false
                            selectedLessonId = null
                            onStartMindfulness(selectedLesson.id)
                        },
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun MindfulnessCourseList(
    completedLessonIds: Set<Int>,
    onSelectLesson: (MindfulnessLesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            stringResource(R.string.mindfulness_courses_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.mindfulness_desc),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        MindfulnessLessons.all.forEachIndexed { index, lesson ->
            val completed = lesson.id in completedLessonIds
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectLesson(lesson) }
                    .padding(vertical = 12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (completed) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (completed) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        } else {
                            Text(lesson.id.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(lesson.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (completed) {
                            stringResource(R.string.mindfulness_lesson_completed)
                        } else {
                            stringResource(lesson.durationRes)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (completed) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index < MindfulnessLessons.all.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 54.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.mindfulness_source_credit),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MindfulnessLessonDetail(
    lesson: MindfulnessLesson,
    completed: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.mindfulness_courses_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Surface(
            shape = CircleShape,
            color = if (completed) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Text(lesson.id.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            stringResource(lesson.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(lesson.durationRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(lesson.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(R.string.mindfulness_source_credit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(if (completed) Icons.Rounded.Replay else Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (completed) R.string.mindfulness_replay else R.string.mindfulness_start_lesson))
        }
        Spacer(Modifier.height(24.dp))
    }
}
