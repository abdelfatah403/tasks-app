package com.taskflow.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.app.data.local.TaskFlowDatabase
import com.taskflow.app.data.model.FocusSessionEntity
import com.taskflow.app.data.model.HabitEntity
import com.taskflow.app.data.model.HabitLogEntity
import com.taskflow.app.data.model.NoteEntity
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TaskEntity
import com.taskflow.app.data.model.TaskPriority
import com.taskflow.app.data.repository.TaskFlowRepository
import com.taskflow.app.domain.SmartQuickAddParser
import com.taskflow.app.widget.TaskFlowWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TaskFilterMode(val label: String) {
    ALL("All Tasks"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    STARRED("Starred"),
    COMPLETED("Completed")
}

enum class PomodoroPhase(val label: String, val defaultMinutes: Int) {
    FOCUS_25("Focus • 25m", 25),
    DEEP_50("Deep Work • 50m", 50),
    SHORT_BREAK("Short Break • 5m", 5),
    LONG_BREAK("Long Break • 15m", 15)
}

data class PomodoroTimerState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS_25,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val linkedTaskId: Long? = null,
    val linkedTaskTitle: String = "Select a task to focus on",
    val ambientSoundName: String = "Off"
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
        } else 0f

    val formattedClock: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TaskFlowDatabase.getInstance(application)
    private val repository = TaskFlowRepository(database.taskFlowDao())

    val projects: StateFlow<List<ProjectEntity>> = repository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubtasks: StateFlow<List<SubtaskEntity>> = repository.subtasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.habitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogs: StateFlow<List<HabitLogEntity>> = repository.habitLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = repository.focusSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Filters & Selection (Defaults to ALL so all tasks across all projects/tags show by default)
    private val _selectedFilter = MutableStateFlow(TaskFilterMode.ALL)
    val selectedFilter: StateFlow<TaskFilterMode> = _selectedFilter.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCalendarEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedCalendarEpochDay: StateFlow<Long> = _selectedCalendarEpochDay.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Pomodoro State
    private val _pomodoroState = MutableStateFlow(PomodoroTimerState())
    val pomodoroState: StateFlow<PomodoroTimerState> = _pomodoroState.asStateFlow()
    private var timerJob: Job? = null

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _selectedFilter,
        _selectedProjectId,
        _selectedTag,
        _searchQuery
    ) { tasks, filter, projectId, tag, query ->
        val today = LocalDate.now().toEpochDay()
        tasks.filter { task ->
            val matchesSearch = query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.notes.contains(query, ignoreCase = true) ||
                task.tagsCsv.contains(query, ignoreCase = true)

            val matchesProject = projectId == null || task.projectId == projectId
            val matchesTag = tag == null || task.tagsList.any { it.equals(tag, ignoreCase = true) }

            val matchesFilter = when (filter) {
                TaskFilterMode.TODAY -> !task.isCompleted && (task.dueDateEpochDay == null || task.dueDateEpochDay <= today)
                TaskFilterMode.UPCOMING -> !task.isCompleted && (task.dueDateEpochDay != null && task.dueDateEpochDay > today)
                TaskFilterMode.STARRED -> !task.isCompleted && task.isStarred
                TaskFilterMode.ALL -> !task.isCompleted
                TaskFilterMode.COMPLETED -> task.isCompleted
            }

            matchesSearch && matchesProject && matchesTag && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterMode(mode: TaskFilterMode) {
        _selectedFilter.value = mode
        if (mode == TaskFilterMode.ALL) {
            // When user chooses "All Tasks", reset project & tag filters so all tasks in all projects/tags are shown
            _selectedProjectId.value = null
            _selectedTag.value = null
        }
    }

    fun selectProjectFilter(projectId: Long?) {
        if (projectId == null) {
            _selectedProjectId.value = null
            _selectedTag.value = null
        } else {
            _selectedProjectId.value = if (_selectedProjectId.value == projectId) null else projectId
        }
    }

    fun selectTagFilter(tag: String?) {
        if (tag == null) {
            _selectedTag.value = null
        } else {
            _selectedTag.value = if (_selectedTag.value.equals(tag, ignoreCase = true)) null else tag
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCalendarDay(epochDay: Long) {
        _selectedCalendarEpochDay.value = epochDay
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun postBannerMessage(msg: String) {
        _statusBannerMessage.value = msg
    }

    // --- Smart Quick-Add Task Submission ---
    fun addTaskFromSmartInput(
        rawInput: String,
        notes: String,
        overridePriority: TaskPriority?,
        overrideProjectId: Long?,
        overrideEpochDay: Long?,
        overrideTimeMinutes: Int?,
        overrideRecurrence: RecurrenceType?,
        overrideStarred: Boolean?,
        subtaskLines: List<String>
    ) {
        if (rawInput.isBlank()) return
        viewModelScope.launch {
            val availableProjects = projects.value
            val parsed = SmartQuickAddParser.parse(rawInput, availableProjects)

            val finalProject = when {
                overrideProjectId != null -> availableProjects.find { it.id == overrideProjectId }
                parsed.projectId != null -> availableProjects.find { it.id == parsed.projectId }
                else -> availableProjects.firstOrNull()
            }

            val finalPriority = overridePriority ?: parsed.priority
            val finalDueDate = overrideEpochDay ?: parsed.dueDateEpochDay ?: LocalDate.now().toEpochDay()
            val finalDueTime = overrideTimeMinutes ?: parsed.dueTimeMinutes
            val finalRecurrence = overrideRecurrence ?: parsed.recurrenceType
            val finalStarred = overrideStarred ?: parsed.isStarred

            val entity = TaskEntity(
                title = parsed.cleanTitle.ifBlank { rawInput.trim() },
                notes = notes.trim(),
                projectId = finalProject?.id ?: 1L,
                projectName = finalProject?.name ?: (parsed.projectName ?: "Inbox"),
                priorityCode = finalPriority.code,
                dueDateEpochDay = finalDueDate,
                dueTimeMinutes = finalDueTime,
                isStarred = finalStarred,
                recurrenceType = finalRecurrence.name,
                recurrenceIntervalDays = parsed.recurrenceIntervalDays,
                tagsCsv = parsed.tags.joinToString(","),
                estimatedPomodoros = parsed.estimatedPomodoros
            )

            repository.addTask(entity, subtaskLines)
            refreshHomeWidget()
            _statusBannerMessage.value = "Added \"${entity.title}\""
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val nextRecurring = repository.toggleTaskCompletion(task)
            refreshHomeWidget()
            if (nextRecurring != null) {
                val nextDateStr = nextRecurring.dueDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "next cycle"
                _statusBannerMessage.value = "Completed! Next recurring task scheduled for $nextDateStr"
            }
        }
    }

    fun toggleTaskStar(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskStar(task)
            refreshHomeWidget()
        }
    }

    fun changeTaskPriority(task: TaskEntity, priority: TaskPriority) {
        viewModelScope.launch {
            repository.updateTaskPriority(task, priority.code)
            refreshHomeWidget()
        }
    }

    fun rescheduleTask(task: TaskEntity, newEpochDay: Long?) {
        viewModelScope.launch {
            repository.rescheduleTask(task, newEpochDay)
            refreshHomeWidget()
        }
    }

    fun updateFullTask(updatedTask: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(updatedTask)
            refreshHomeWidget()
            _statusBannerMessage.value = "Updated \"${updatedTask.title}\""
        }
    }

    private var lastDeletedTask: TaskEntity? = null
    private var lastDeletedSubtasks: List<SubtaskEntity> = emptyList()

    private val _canUndoDelete = MutableStateFlow(false)
    val canUndoDelete: StateFlow<Boolean> = _canUndoDelete.asStateFlow()

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            lastDeletedTask = task
            lastDeletedSubtasks = allSubtasks.value.filter { it.taskId == task.id }
            _canUndoDelete.value = true
            repository.deleteTask(task)
            refreshHomeWidget()
            _statusBannerMessage.value = "Deleted \"${task.title}\""
        }
    }

    fun undoDeleteTask() {
        val restoredTask = lastDeletedTask ?: return
        val restoredSubs = lastDeletedSubtasks
        lastDeletedTask = null
        lastDeletedSubtasks = emptyList()
        _canUndoDelete.value = false
        viewModelScope.launch {
            repository.addTask(restoredTask.copy(id = 0), restoredSubs.map { it.title })
            refreshHomeWidget()
            _statusBannerMessage.value = "Restored \"${restoredTask.title}\""
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
            refreshHomeWidget()
            _statusBannerMessage.value = "Cleared completed tasks"
        }
    }

    // --- Subtasks ---
    fun addSubtask(taskId: Long, title: String) {
        viewModelScope.launch {
            repository.addSubtask(taskId, title)
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.toggleSubtask(subtask)
        }
    }

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.deleteSubtask(subtask)
        }
    }

    // --- Projects ---
    fun addProject(name: String, colorHex: Long) {
        viewModelScope.launch {
            repository.addProject(name, colorHex)
        }
    }

    // --- Habits ---
    fun addHabit(title: String, emoji: String, colorHex: Long, targetDays: Int) {
        viewModelScope.launch {
            repository.addHabit(title, emoji, colorHex, targetDays)
            _statusBannerMessage.value = "Habit \"$title\" created!"
        }
    }

    fun toggleHabitDay(habitId: Long, epochDay: Long) {
        viewModelScope.launch {
            repository.toggleHabitForDay(habitId, epochDay)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // --- Notes ---
    fun addNote(
        title: String,
        content: String,
        projectId: Long,
        tagsCsv: String,
        colorHex: Long,
        isPinned: Boolean
    ) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val proj = projects.value.find { it.id == projectId } ?: projects.value.firstOrNull()
            val cleanedTags = tagsCsv
                .split(",")
                .map { it.trim().removePrefix("@") }
                .filter { it.isNotEmpty() }
                .joinToString(",")

            val note = NoteEntity(
                title = title.trim().ifBlank { "Untitled Note" },
                content = content.trim(),
                projectId = proj?.id ?: 1L,
                projectName = proj?.name ?: "Inbox",
                tagsCsv = cleanedTags,
                colorHex = colorHex,
                isPinned = isPinned
            )
            repository.addNote(note)
            _statusBannerMessage.value = "Saved note \"${note.title}\""
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
            _statusBannerMessage.value = "Updated note \"${note.title}\""
        }
    }

    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleNotePin(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _statusBannerMessage.value = "Deleted note \"${note.title}\""
        }
    }

    fun convertNoteToTask(note: NoteEntity) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = note.title,
                notes = note.content,
                projectId = note.projectId,
                projectName = note.projectName,
                priorityCode = TaskPriority.P2.code,
                dueDateEpochDay = LocalDate.now().toEpochDay(),
                tagsCsv = note.tagsCsv
            )
            repository.addTask(task)
            refreshHomeWidget()
            _statusBannerMessage.value = "Converted \"${note.title}\" into Today's Task!"
        }
    }

    // --- Pomodoro Timer Engine ---
    fun selectTaskForPomodoro(task: TaskEntity) {
        _pomodoroState.value = _pomodoroState.value.copy(
            linkedTaskId = task.id,
            linkedTaskTitle = task.title
        )
    }

    fun setPomodoroPhase(phase: PomodoroPhase) {
        timerJob?.cancel()
        val secs = phase.defaultMinutes * 60
        _pomodoroState.value = _pomodoroState.value.copy(
            phase = phase,
            totalSeconds = secs,
            remainingSeconds = secs,
            isRunning = false
        )
    }

    fun cycleAmbientSound() {
        val options = listOf("Off", "Rainforest 🌧️", "Coffee Shop ☕", "White Noise 🌊", "Binaural 40Hz 🎧")
        val current = _pomodoroState.value.ambientSoundName
        val nextIndex = (options.indexOf(current) + 1) % options.size
        _pomodoroState.value = _pomodoroState.value.copy(ambientSoundName = options[nextIndex])
    }

    fun togglePomodoroStartPause() {
        val current = _pomodoroState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _pomodoroState.value = current.copy(isRunning = false)
        } else {
            _pomodoroState.value = current.copy(isRunning = true)
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_pomodoroState.value.remainingSeconds > 0 && _pomodoroState.value.isRunning) {
                    delay(1000L)
                    val s = _pomodoroState.value
                    if (!s.isRunning) break
                    val nextRem = s.remainingSeconds - 1
                    if (nextRem <= 0) {
                        onPomodoroCompleted()
                        break
                    } else {
                        _pomodoroState.value = s.copy(remainingSeconds = nextRem)
                    }
                }
            }
        }
    }

    fun resetPomodoro() {
        timerJob?.cancel()
        val current = _pomodoroState.value
        _pomodoroState.value = current.copy(
            remainingSeconds = current.totalSeconds,
            isRunning = false
        )
    }

    fun completePomodoroNowForTesting() {
        viewModelScope.launch {
            onPomodoroCompleted()
        }
    }

    private suspend fun onPomodoroCompleted() {
        timerJob?.cancel()
        val state = _pomodoroState.value
        val mins = (state.totalSeconds / 60).coerceAtLeast(1)
        if (state.phase == PomodoroPhase.FOCUS_25 || state.phase == PomodoroPhase.DEEP_50) {
            repository.recordFocusSession(
                taskId = state.linkedTaskId,
                taskTitle = state.linkedTaskTitle,
                durationMinutes = mins
            )
            _statusBannerMessage.value = "🎉 Focus session complete (+$mins mins logged)!"
        } else {
            _statusBannerMessage.value = "Break finished! Ready for your next focus sprint."
        }
        _pomodoroState.value = state.copy(
            remainingSeconds = state.totalSeconds,
            isRunning = false
        )
    }

    // --- Offline JSON Backup Export & Restore ---
    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson()
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(rawJson)
            result.onSuccess { count ->
                refreshHomeWidget()
                _statusBannerMessage.value = "Restored $count items from local backup!"
            }.onFailure { err ->
                _statusBannerMessage.value = "Backup restore failed: ${err.localizedMessage ?: "Invalid JSON"}"
            }
        }
    }

    private fun refreshHomeWidget() {
        TaskFlowWidgetProvider.requestWidgetUpdate(getApplication())
    }
}
