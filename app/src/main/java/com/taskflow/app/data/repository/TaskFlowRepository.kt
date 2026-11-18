package com.taskflow.app.data.repository

import com.taskflow.app.data.local.TaskFlowDao
import com.taskflow.app.data.model.BackupPayload
import com.taskflow.app.data.model.FocusSessionEntity
import com.taskflow.app.data.model.HabitEntity
import com.taskflow.app.data.model.HabitLogEntity
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

class TaskFlowRepository(private val dao: TaskFlowDao) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val projectsFlow: Flow<List<ProjectEntity>> = dao.observeProjects()
    val tasksFlow: Flow<List<TaskEntity>> = dao.observeAllTasks()
    val subtasksFlow: Flow<List<SubtaskEntity>> = dao.observeAllSubtasks()
    val habitsFlow: Flow<List<HabitEntity>> = dao.observeAllHabits()
    val habitLogsFlow: Flow<List<HabitLogEntity>> = dao.observeAllHabitLogs()
    val focusSessionsFlow: Flow<List<FocusSessionEntity>> = dao.observeFocusSessions()

    suspend fun addTask(task: TaskEntity, subtaskTitles: List<String> = emptyList()): Long {
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

    // --- Projects ---
    suspend fun addProject(name: String, colorHex: Long) {
        if (name.isBlank()) return
        dao.insertProject(ProjectEntity(name = name.trim(), colorHex = colorHex))
    }

    suspend fun deleteProject(project: ProjectEntity) {
        if (!project.isDefault) {
            dao.deleteProject(project)
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

    // --- 100% Local JSON Backup Export & Restore ---
    suspend fun exportBackupJson(): String {
        val payload = BackupPayload(
            version = 1,
            exportedAtMillis = System.currentTimeMillis(),
            projects = dao.getAllProjectsSnapshot(),
            tasks = dao.getAllTasksSnapshot(),
            subtasks = dao.getAllSubtasksSnapshot(),
            habits = dao.getAllHabitsSnapshot(),
            habitLogs = dao.getAllHabitLogsSnapshot(),
            focusSessions = dao.getAllFocusSessionsSnapshot()
        )
        return json.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> {
        return runCatching {
            val payload = json.decodeFromString<BackupPayload>(rawJson)
            dao.restoreFullBackup(
                projects = payload.projects,
                tasks = payload.tasks,
                subtasks = payload.subtasks,
                habits = payload.habits,
                habitLogs = payload.habitLogs,
                focusSessions = payload.focusSessions
            )
            payload.tasks.size + payload.habits.size
        }
    }
}
