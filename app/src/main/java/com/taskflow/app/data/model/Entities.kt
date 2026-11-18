package com.taskflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TaskPriority(
    val code: Int,
    val label: String,
    val shortTag: String,
    val quadrantTitle: String,
    val quadrantSubtitle: String,
    val colorHex: Long
) {
    P1(
        code = 1,
        label = "P1 • Urgent & Important",
        shortTag = "P1",
        quadrantTitle = "Do First",
        quadrantSubtitle = "Urgent & Important",
        colorHex = 0xFFEF4444
    ),
    P2(
        code = 2,
        label = "P2 • Important, Not Urgent",
        shortTag = "P2",
        quadrantTitle = "Schedule",
        quadrantSubtitle = "Important, Not Urgent",
        colorHex = 0xFFF59E0B
    ),
    P3(
        code = 3,
        label = "P3 • Urgent, Unimportant",
        shortTag = "P3",
        quadrantTitle = "Delegate",
        quadrantSubtitle = "Urgent, Unimportant",
        colorHex = 0xFF3B82F6
    ),
    P4(
        code = 4,
        label = "P4 • Neither / Someday",
        shortTag = "P4",
        quadrantTitle = "Eliminate / Later",
        quadrantSubtitle = "Not Urgent & Unimportant",
        colorHex = 0xFF64748B
    );

    companion object {
        fun fromCode(code: Int): TaskPriority = entries.find { it.code == code } ?: P4
    }
}

@Serializable
enum class RecurrenceType(val label: String) {
    NONE("One-time"),
    DAILY("Daily"),
    WEEKDAYS("Every Weekday (Mon–Fri)"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM_DAYS("Every N Days")
}

@Serializable
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: Long,
    val iconName: String = "folder",
    val isDefault: Boolean = false
)

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val projectId: Long = 1L,
    val projectName: String = "Inbox",
    val priorityCode: Int = 4, // 1=P1, 2=P2, 3=P3, 4=P4
    val dueDateEpochDay: Long? = null, // LocalDate.toEpochDay()
    val dueTimeMinutes: Int? = null, // Minutes from midnight (e.g., 9*60 + 30 = 09:30)
    val isStarred: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val recurrenceType: String = RecurrenceType.NONE.name,
    val recurrenceIntervalDays: Int = 1,
    val tagsCsv: String = "", // Comma-separated tags without '@'
    val estimatedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val focusedMinutes: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val priority: TaskPriority
        get() = TaskPriority.fromCode(priorityCode)

    val recurrence: RecurrenceType
        get() = runCatching { RecurrenceType.valueOf(recurrenceType) }.getOrDefault(RecurrenceType.NONE)

    val tagsList: List<String>
        get() = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val formattedTime: String?
        get() = dueTimeMinutes?.let {
            val h = it / 60
            val m = it % 60
            val amPm = if (h >= 12) "PM" else "AM"
            val hour12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            "%d:%02d %s".format(hour12, m, amPm)
        }
}

@Serializable
@Entity(tableName = "subtasks")
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false
)

@Serializable
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "🔥",
    val colorHex: Long = 0xFF10B981,
    val targetDaysPerWeek: Int = 7,
    val reminderTimeMinutes: Int? = 8 * 60,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "epochDay"])
data class HabitLogEntity(
    val habitId: Long,
    val epochDay: Long,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String = "Deep Work Session",
    val durationMinutes: Int,
    val epochDay: Long,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val projects: List<ProjectEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val subtasks: List<SubtaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList()
)
