package com.taskflow.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.taskflow.app.data.model.FocusSessionEntity
import com.taskflow.app.data.model.HabitEntity
import com.taskflow.app.data.model.HabitLogEntity
import com.taskflow.app.data.model.NoteEntity
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TaskEntity
import com.taskflow.app.ui.PomodoroPhase
import com.taskflow.app.ui.PomodoroTimerState
import com.taskflow.app.ui.TaskFilterMode
import com.taskflow.app.ui.theme.TaskFlowTheme
import java.time.LocalDate

private val previewToday = LocalDate.now().toEpochDay()

private val sampleProjects = listOf(
    ProjectEntity(1, "Inbox", 0xFF6366F1, isDefault = true),
    ProjectEntity(2, "Work", 0xFFEC4899),
    ProjectEntity(3, "Fitness", 0xFFF59E0B)
)

private val sampleTasks = listOf(
    TaskEntity(
        id = 1,
        title = "Finalize Q4 Android architecture & sprint roadmap",
        notes = "Review Jetpack Compose metrics and offline Room sync",
        projectId = 2,
        projectName = "Work",
        priorityCode = 1,
        dueDateEpochDay = previewToday,
        dueTimeMinutes = 10 * 60 + 30,
        isStarred = true,
        recurrenceType = RecurrenceType.WEEKLY.name,
        tagsCsv = "deepwork,architecture",
        estimatedPomodoros = 3,
        completedPomodoros = 1
    ),
    TaskEntity(
        id = 2,
        title = "45-min Strength Training + Mobility",
        projectId = 3,
        projectName = "Fitness",
        priorityCode = 2,
        dueDateEpochDay = previewToday,
        dueTimeMinutes = 18 * 60,
        isStarred = true,
        recurrenceType = RecurrenceType.DAILY.name,
        tagsCsv = "health",
        estimatedPomodoros = 2
    ),
    TaskEntity(
        id = 3,
        title = "Reply to vendor invoice & shipment emails",
        projectId = 2,
        projectName = "Work",
        priorityCode = 3,
        dueDateEpochDay = previewToday,
        dueTimeMinutes = 14 * 60,
        tagsCsv = "email,admin"
    )
)

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun TasksScreenPreview() {
    TaskFlowTheme(darkTheme = true) {
        TasksScreen(
            tasks = sampleTasks,
            allTasks = sampleTasks,
            subtasks = listOf(
                SubtaskEntity(1, 1, "Audit Room DB indexing", true),
                SubtaskEntity(2, 1, "Verify Smart NLP parser", false)
            ),
            projects = sampleProjects,
            selectedFilter = TaskFilterMode.TODAY,
            selectedProjectId = null,
            selectedTag = null,
            searchQuery = "",
            onSelectFilter = {},
            onSelectProject = {},
            onSelectTag = {},
            onSearchChange = {},
            onToggleComplete = {},
            onToggleStar = {},
            onDeleteTask = {},
            onChangePriority = { _, _ -> },
            onRescheduleTask = { _, _ -> },
            onAddSubtask = { _, _ -> },
            onToggleSubtask = {},
            onDeleteSubtask = {},
            onStartFocusForTask = {},
            onOpenVoiceQuickAdd = {},
            onClearCompleted = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun EisenhowerMatrixPreview() {
    TaskFlowTheme(darkTheme = true) {
        EisenhowerMatrixScreen(
            allTasks = sampleTasks,
            onToggleComplete = {},
            onMovePriority = { _, _ -> },
            onQuickAddInQuadrant = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun PomodoroFocusPreview() {
    TaskFlowTheme(darkTheme = true) {
        PomodoroFocusScreen(
            timerState = PomodoroTimerState(
                phase = PomodoroPhase.FOCUS_25,
                totalSeconds = 1500,
                remainingSeconds = 1125,
                isRunning = true,
                linkedTaskId = 1,
                linkedTaskTitle = "Finalize Q4 Android architecture & sprint roadmap",
                ambientSoundName = "Rainforest 🌧️"
            ),
            activeTasks = sampleTasks,
            focusSessions = listOf(
                FocusSessionEntity(1, 1, "Finalize Q4 Android architecture", 25, previewToday)
            ),
            onSelectPhase = {},
            onSelectTask = {},
            onToggleStartPause = {},
            onReset = {},
            onCycleAmbientSound = {},
            onCompleteSessionNow = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun HabitsAndStatsPreview() {
    TaskFlowTheme(darkTheme = true) {
        HabitsAndStatsScreen(
            habits = listOf(
                HabitEntity(1, "Read 20 pages of non-fiction", "📚", 0xFF6366F1, 7),
                HabitEntity(2, "Drink 2.5L Hydration", "💧", 0xFF06B6D4, 7)
            ),
            habitLogs = listOf(
                HabitLogEntity(1, previewToday),
                HabitLogEntity(1, previewToday - 1),
                HabitLogEntity(2, previewToday)
            ),
            allTasks = sampleTasks,
            focusSessions = listOf(FocusSessionEntity(1, 1, "Deep Work", 50, previewToday)),
            onToggleHabitDay = { _, _ -> },
            onAddHabit = { _, _, _, _ -> },
            onDeleteHabit = {},
            onExportBackupJson = { "{}" },
            onImportBackupJson = {},
            onShowMessage = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun NotesScreenPreview() {
    TaskFlowTheme(darkTheme = true) {
        NotesScreen(
            notes = listOf(
                NoteEntity(
                    id = 1,
                    title = "Q4 Product Launch & Architecture Notes",
                    content = "• Prioritize 100% offline Room DB speed\n• Keep Smart NLP Quick-Add under 50ms\n• Support direct 1-tap Edit & Delete on every card",
                    projectId = 2,
                    projectName = "Work",
                    tagsCsv = "architecture,roadmap",
                    colorHex = 0xFF6366F1,
                    isPinned = true
                ),
                NoteEntity(
                    id = 2,
                    title = "Atomic Habits — Key Takeaways",
                    content = "1. Make it obvious\n2. Make it attractive\n3. Make it easy (2-minute rule)",
                    projectId = 1,
                    projectName = "Inbox",
                    tagsCsv = "books,mindset",
                    colorHex = 0xFF10B981,
                    isPinned = false
                )
            ),
            projects = sampleProjects,
            onAddNote = { _, _, _, _, _, _ -> },
            onUpdateNote = {},
            onTogglePin = {},
            onDeleteNote = {},
            onConvertToTask = {}
        )
    }
}
