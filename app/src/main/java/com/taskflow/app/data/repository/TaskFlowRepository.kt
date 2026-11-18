package com.taskflow.app.data.repository

import com.taskflow.app.data.local.TaskFlowDao
import com.taskflow.app.data.model.BackupPayload
import com.taskflow.app.data.model.FocusSessionEntity
import com.taskflow.app.data.model.HabitEntity
import com.taskflow.app.data.model.HabitLogEntity
import com.taskflow.app.data.model.NoteEntity
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TagEntity
import com.taskflow.app.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

class TaskFlowRepository(private val dao: TaskFlowDao) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    val projectsFlow: Flow<List<ProjectEntity>> = dao.observeProjects()
    val tagsFlow: Flow<List<TagEntity>> = dao.observeTags()
    val tasksFlow: Flow<List<TaskEntity>> = dao.observeAllTasks()
    val subtasksFlow: Flow<List<SubtaskEntity>> = dao.observeAllSubtasks()
    val habitsFlow: Flow<List<HabitEntity>> = dao.observeAllHabits()
    val habitLogsFlow: Flow<List<HabitLogEntity>> = dao.observeAllHabitLogs()
    val focusSessionsFlow: Flow<List<FocusSessionEntity>> = dao.observeFocusSessions()
    val notesFlow: Flow<List<NoteEntity>> = dao.observeAllNotes()

    suspend fun addTask(task: TaskEntity, subtaskTitles: List<String> = emptyList()): Long {
        syncTagsToTable(task.tagsList)
        val taskId = dao.insertTask(task)
        if (subtaskTitles.isNotEmpty()) {
            val entities = subtaskTitles
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { SubtaskEntity(taskId = taskId, title = it) }
            dao.insertSubtasks(entities)
        }
        return taskId
    }

    suspend fun updateTask(task: TaskEntity) {
        syncTagsToTable(task.tagsList)
        dao.updateTask(task)
    }

    /**
     * Toggles task completion. If the task is recurring and is being marked completed,
     * automatically spawns the next occurrence according to its RecurrenceType (like Todoist & TickTick).
     */
    suspend fun toggleTaskCompletion(task: TaskEntity): TaskEntity? {
        val nowCompleted = !task.isCompleted
        val updated = task.copy(
            isCompleted = nowCompleted,
            completedAtMillis = if (nowCompleted) System.currentTimeMillis() else null
        )
        dao.updateTask(updated)

        if (nowCompleted && task.recurrence != RecurrenceType.NONE) {
            val baseEpochDay = task.dueDateEpochDay ?: LocalDate.now().toEpochDay()
            val baseDate = LocalDate.ofEpochDay(baseEpochDay)
            val nextDate = calculateNextOccurrence(
                baseDate = baseDate,
                recurrence = task.recurrence,
                intervalDays = task.recurrenceIntervalDays
            )
            val nextTask = task.copy(
                id = 0,
                isCompleted = false,
                completedAtMillis = null,
                dueDateEpochDay = nextDate.toEpochDay(),
                completedPomodoros = 0,
                focusedMinutes = 0,
                createdAtMillis = System.currentTimeMillis()
            )
            dao.insertTask(nextTask)
            return nextTask
        }
        return null
    }

    private fun calculateNextOccurrence(
        baseDate: LocalDate,
        recurrence: RecurrenceType,
        intervalDays: Int
    ): LocalDate {
        return when (recurrence) {
            RecurrenceType.NONE -> baseDate
            RecurrenceType.DAILY -> baseDate.plusDays(1)
            RecurrenceType.WEEKDAYS -> {
                var candidate = baseDate.plusDays(1)
                while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }
            RecurrenceType.WEEKLY -> baseDate.plusWeeks(1)
            RecurrenceType.MONTHLY -> baseDate.plusMonths(1)
            RecurrenceType.CUSTOM_DAYS -> baseDate.plusDays(intervalDays.coerceAtLeast(1).toLong())
        }
    }

    suspend fun toggleTaskStar(task: TaskEntity) {
        dao.updateTask(task.copy(isStarred = !task.isStarred))
    }

    suspend fun updateTaskPriority(task: TaskEntity, newPriorityCode: Int) {
        dao.updateTask(task.copy(priorityCode = newPriorityCode.coerceIn(1, 4)))
    }

    suspend fun rescheduleTask(task: TaskEntity, newEpochDay: Long?) {
        dao.updateTask(task.copy(dueDateEpochDay = newEpochDay))
    }

    suspend fun deleteTask(task: TaskEntity) {
        dao.deleteSubtasksForTask(task.id)
        dao.deleteTask(task)
    }

    suspend fun clearCompletedTasks() {
        dao.clearCompletedTasks()
    }

    // --- Subtasks ---
    suspend fun addSubtask(taskId: Long, title: String) {
        if (title.isBlank()) return
        dao.insertSubtask(SubtaskEntity(taskId = taskId, title = title.trim()))
    }

    suspend fun toggleSubtask(subtask: SubtaskEntity) {
        dao.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
    }

    suspend fun deleteSubtask(subtask: SubtaskEntity) {
        dao.deleteSubtask(subtask)
    }

    // --- Projects (Add, Update/Rename, Delete) ---
    suspend fun addProject(name: String, colorHex: Long) {
        if (name.isBlank()) return
        dao.insertProject(ProjectEntity(name = name.trim(), colorHex = colorHex))
    }

    suspend fun updateProject(project: ProjectEntity, newName: String, newColorHex: Long) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        val updated = project.copy(name = cleanName, colorHex = newColorHex)
        dao.updateProject(updated)
        dao.updateTaskProjectNames(project.id, cleanName)
        dao.updateNoteProjectNames(project.id, cleanName)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        if (project.id == 1L && project.isDefault) return
        dao.reassignTasksToInbox(project.id)
        dao.reassignNotesToInbox(project.id)
        dao.deleteProject(project)
    }

    // --- Tags (Add, Update/Rename across all Tasks & Notes, Delete across all Tasks & Notes) ---
    private suspend fun syncTagsToTable(tagNames: List<String>) {
        if (tagNames.isEmpty()) return
        val existing = dao.getAllTagsSnapshot().map { it.name.lowercase() }.toSet()
        val newTags = tagNames
            .map { it.trim().removePrefix("@").lowercase() }
            .filter { it.isNotEmpty() && !existing.contains(it) }
            .distinct()
            .map { TagEntity(name = it) }
        if (newTags.isNotEmpty()) {
            dao.insertTags(newTags)
        }
    }

    suspend fun addTag(name: String, colorHex: Long = 0xFF8B5CF6) {
        val clean = name.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) return
        val existing = dao.getAllTagsSnapshot().find { it.name.equals(clean, ignoreCase = true) }
        if (existing == null) {
            dao.insertTag(TagEntity(name = clean, colorHex = colorHex))
        }
    }

    suspend fun renameTag(oldName: String, newName: String, newColorHex: Long = 0xFF8B5CF6) {
        val cleanOld = oldName.trim().removePrefix("@").lowercase()
        val cleanNew = newName.trim().removePrefix("@").lowercase()
        if (cleanOld.isBlank() || cleanNew.isBlank()) return

        val allTags = dao.getAllTagsSnapshot()
        val targetTag = allTags.find { it.name.equals(cleanOld, ignoreCase = true) }
        if (targetTag != null) {
            dao.updateTag(targetTag.copy(name = cleanNew, colorHex = newColorHex))
        } else {
            dao.insertTag(TagEntity(name = cleanNew, colorHex = newColorHex))
        }

        // Also update tagsCsv on all Tasks & Notes that used @oldName
        val tasks = dao.getAllTasksSnapshot()
        tasks.forEach { task ->
            val updatedList = task.tagsList.map {
                if (it.equals(cleanOld, ignoreCase = true)) cleanNew else it
            }.distinct()
            val newCsv = updatedList.joinToString(",")
            if (newCsv != task.tagsCsv) {
                dao.updateTask(task.copy(tagsCsv = newCsv))
            }
        }

        val notes = dao.getAllNotesSnapshot()
        notes.forEach { note ->
            val updatedList = note.tagsList.map {
                if (it.equals(cleanOld, ignoreCase = true)) cleanNew else it
            }.distinct()
            val newCsv = updatedList.joinToString(",")
            if (newCsv != note.tagsCsv) {
                dao.updateNote(note.copy(tagsCsv = newCsv))
            }
        }
    }

    suspend fun deleteTag(tagName: String) {
        val clean = tagName.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) return
        dao.deleteTagByName(clean)

        // Also remove @tagName from all Tasks & Notes
        val tasks = dao.getAllTasksSnapshot()
        tasks.forEach { task ->
            val updatedList = task.tagsList.filterNot { it.equals(clean, ignoreCase = true) }
            val newCsv = updatedList.joinToString(",")
            if (newCsv != task.tagsCsv) {
                dao.updateTask(task.copy(tagsCsv = newCsv))
            }
        }

        val notes = dao.getAllNotesSnapshot()
        notes.forEach { note ->
            val updatedList = note.tagsList.filterNot { it.equals(clean, ignoreCase = true) }
            val newCsv = updatedList.joinToString(",")
            if (newCsv != note.tagsCsv) {
                dao.updateNote(note.copy(tagsCsv = newCsv))
            }
        }
    }

    // --- Habits ---
    suspend fun addHabit(title: String, emoji: String, colorHex: Long, targetDaysPerWeek: Int) {
        if (title.isBlank()) return
        dao.insertHabit(
            HabitEntity(
                title = title.trim(),
                emoji = emoji.ifBlank { "🔥" },
                colorHex = colorHex,
                targetDaysPerWeek = targetDaysPerWeek.coerceIn(1, 7)
            )
        )
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        dao.deleteHabit(habit)
    }

    suspend fun toggleHabitForDay(habitId: Long, epochDay: Long) {
        val existing = dao.getHabitLog(habitId, epochDay)
        if (existing != null) {
            dao.deleteHabitLog(habitId, epochDay)
        } else {
            dao.insertHabitLog(HabitLogEntity(habitId = habitId, epochDay = epochDay))
        }
    }

    // --- Pomodoro Focus Sessions ---
    suspend fun recordFocusSession(taskId: Long?, taskTitle: String, durationMinutes: Int) {
        val today = LocalDate.now().toEpochDay()
        dao.insertFocusSession(
            FocusSessionEntity(
                taskId = taskId,
                taskTitle = taskTitle.ifBlank { "Deep Focus Session" },
                durationMinutes = durationMinutes,
                epochDay = today
            )
        )
        if (taskId != null) {
            val task = dao.getTaskById(taskId)
            if (task != null) {
                dao.updateTask(
                    task.copy(
                        completedPomodoros = task.completedPomodoros + 1,
                        focusedMinutes = task.focusedMinutes + durationMinutes
                    )
                )
            }
        }
    }

    // --- Notes ---
    suspend fun addNote(note: NoteEntity): Long {
        syncTagsToTable(note.tagsList)
        return dao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        syncTagsToTable(note.tagsList)
        dao.updateNote(note.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun toggleNotePin(note: NoteEntity) {
        dao.updateNote(
            note.copy(
                isPinned = !note.isPinned,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNote(note: NoteEntity) {
        dao.deleteNote(note)
    }

    // --- 100% Local JSON Backup Export & Restore ---
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            version = 3,
            exportedAtMillis = System.currentTimeMillis(),
            projects = dao.getAllProjectsSnapshot(),
            tags = dao.getAllTagsSnapshot(),
            tasks = dao.getAllTasksSnapshot(),
            subtasks = dao.getAllSubtasksSnapshot(),
            habits = dao.getAllHabitsSnapshot(),
            habitLogs = dao.getAllHabitLogsSnapshot(),
            focusSessions = dao.getAllFocusSessionsSnapshot(),
            notes = dao.getAllNotesSnapshot()
        )
        json.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = rawJson.trim().removePrefix("\uFEFF")
            val startBrace = trimmed.indexOf('{')
            val endBrace = trimmed.lastIndexOf('}')
            require(startBrace >= 0 && endBrace > startBrace) {
                "No valid JSON object found in backup text"
            }
            val cleanJson = trimmed.substring(startBrace, endBrace + 1)
            val payload = json.decodeFromString<BackupPayload>(cleanJson)

            val restoredProjects = payload.projects.ifEmpty {
                listOf(
                    ProjectEntity(id = 1, name = "Inbox", colorHex = 0xFF6366F1, iconName = "inbox", isDefault = true)
                )
            }

            // Collect tags from payload.tags + any tags embedded in tasks/notes
            val embeddedTagNames = (payload.tasks.flatMap { it.tagsList } + payload.notes.flatMap { it.tagsList })
                .map { it.lowercase() }
                .distinct()
            val payloadTagNames = payload.tags.map { it.name.lowercase() }.toSet()
            val mergedTags = payload.tags + embeddedTagNames
                .filterNot { payloadTagNames.contains(it) }
                .map { TagEntity(name = it) }

            dao.restoreFullBackup(
                projects = restoredProjects,
                tags = mergedTags,
                tasks = payload.tasks,
                subtasks = payload.subtasks,
                habits = payload.habits,
                habitLogs = payload.habitLogs,
                focusSessions = payload.focusSessions,
                notes = payload.notes
            )
            payload.tasks.size + payload.habits.size + payload.notes.size + restoredProjects.size
        }
    }
}
