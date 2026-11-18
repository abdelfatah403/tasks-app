package com.taskflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taskflow.app.data.model.FocusSessionEntity
import com.taskflow.app.data.model.HabitEntity
import com.taskflow.app.data.model.HabitLogEntity
import com.taskflow.app.data.model.NoteEntity
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TagEntity
import com.taskflow.app.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        ProjectEntity::class,
        TagEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        FocusSessionEntity::class,
        NoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TaskFlowDatabase : RoomDatabase() {

    abstract fun taskFlowDao(): TaskFlowDao

    companion object {
        @Volatile
        private var INSTANCE: TaskFlowDatabase? = null

        fun getInstance(context: Context): TaskFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskFlowDatabase::class.java,
                    "taskflow_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(SeedDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    seedInitialData(database.taskFlowDao())
                }
            }
        }

        private suspend fun seedInitialData(dao: TaskFlowDao) {
            val today = LocalDate.now().toEpochDay()
            val tomorrow = today + 1
            val inTwoDays = today + 2

            val projects = listOf(
                ProjectEntity(id = 1, name = "Inbox", colorHex = 0xFF6366F1, iconName = "inbox", isDefault = true),
                ProjectEntity(id = 2, name = "Work", colorHex = 0xFFEC4899, iconName = "work", isDefault = false),
                ProjectEntity(id = 3, name = "Personal", colorHex = 0xFF10B981, iconName = "person", isDefault = false),
                ProjectEntity(id = 4, name = "Fitness", colorHex = 0xFFF59E0B, iconName = "fitness", isDefault = false),
                ProjectEntity(id = 5, name = "Shopping", colorHex = 0xFF06B6D4, iconName = "cart", isDefault = false)
            )
            dao.insertProjects(projects)

            val starterTags = listOf(
                TagEntity(name = "deepwork", colorHex = 0xFF6366F1),
                TagEntity(name = "urgent", colorHex = 0xFFEF4444),
                TagEntity(name = "health", colorHex = 0xFF10B981),
                TagEntity(name = "errands", colorHex = 0xFFF59E0B),
                TagEntity(name = "planning", colorHex = 0xFF8B5CF6)
            )
            dao.insertTags(starterTags)

            val task1Id = dao.insertTask(
                TaskEntity(
                    title = "Finalize Q4 Android architecture & sprint roadmap",
                    notes = "Review Jetpack Compose performance metrics and offline Room sync benchmarks",
                    projectId = 2,
                    projectName = "Work",
                    priorityCode = 1, // P1: Urgent & Important
                    dueDateEpochDay = today,
                    dueTimeMinutes = 10 * 60 + 30, // 10:30 AM
                    isStarred = true,
                    recurrenceType = RecurrenceType.NONE.name,
                    tagsCsv = "deepwork,architecture",
                    estimatedPomodoros = 3,
                    completedPomodoros = 1,
                    focusedMinutes = 25
                )
            )

            dao.insertSubtasks(
                listOf(
                    SubtaskEntity(taskId = task1Id, title = "Audit Room DB indexing & DAO flows", isCompleted = true),
                    SubtaskEntity(taskId = task1Id, title = "Verify Smart NLP date parser edge cases", isCompleted = false),
                    SubtaskEntity(taskId = task1Id, title = "Prepare release APK checklist", isCompleted = false)
                )
            )

            dao.insertTask(
                TaskEntity(
                    title = "45-min Strength Training + Mobility",
                    notes = "Upper body push/pull + 10 min stretching",
                    projectId = 4,
                    projectName = "Fitness",
                    priorityCode = 2, // P2: Important, Not Urgent
                    dueDateEpochDay = today,
                    dueTimeMinutes = 18 * 60, // 6:00 PM
                    isStarred = true,
                    recurrenceType = RecurrenceType.DAILY.name,
                    tagsCsv = "health,routine",
                    estimatedPomodoros = 2
                )
            )

            dao.insertTask(
                TaskEntity(
                    title = "Reply to vendor invoice & shipment emails",
                    notes = "Forward accounting receipt PDF",
                    projectId = 2,
                    projectName = "Work",
                    priorityCode = 3, // P3: Urgent, Unimportant (Delegate)
                    dueDateEpochDay = today,
                    dueTimeMinutes = 14 * 60, // 2:00 PM
                    isStarred = false,
                    recurrenceType = RecurrenceType.WEEKDAYS.name,
                    tagsCsv = "email,admin",
                    estimatedPomodoros = 1
                )
            )

            dao.insertTask(
                TaskEntity(
                    title = "Buy espresso beans, oat milk & avocados",
                    notes = "Check local roastery on the way back",
                    projectId = 5,
                    projectName = "Shopping",
                    priorityCode = 4, // P4: Neither
                    dueDateEpochDay = tomorrow,
                    dueTimeMinutes = 17 * 60,
                    isStarred = false,
                    tagsCsv = "errands",
                    estimatedPomodoros = 1
                )
            )

            dao.insertTask(
                TaskEntity(
                    title = "Weekly Sunday Reset & Calendar Review",
                    notes = "Clear Inbox, review Eisenhower quadrants, plan top 3 goals",
                    projectId = 3,
                    projectName = "Personal",
                    priorityCode = 2,
                    dueDateEpochDay = inTwoDays,
                    dueTimeMinutes = 11 * 60,
                    isStarred = true,
                    recurrenceType = RecurrenceType.WEEKLY.name,
                    tagsCsv = "planning,review",
                    estimatedPomodoros = 2
                )
            )

            val h1 = dao.insertHabit(
                HabitEntity(
                    title = "Read 20 pages of non-fiction",
                    emoji = "📚",
                    colorHex = 0xFF6366F1,
                    targetDaysPerWeek = 7
                )
            )
            val h2 = dao.insertHabit(
                HabitEntity(
                    title = "Drink 2.5L Hydration",
                    emoji = "💧",
                    colorHex = 0xFF06B6D4,
                    targetDaysPerWeek = 7
                )
            )
            val h3 = dao.insertHabit(
                HabitEntity(
                    title = "Zero Inbox before 6 PM",
                    emoji = "⚡",
                    colorHex = 0xFF10B981,
                    targetDaysPerWeek = 5
                )
            )
            val h4 = dao.insertHabit(
                HabitEntity(
                    title = "15m Mindful Meditation",
                    emoji = "🧘",
                    colorHex = 0xFFF59E0B,
                    targetDaysPerWeek = 7
                )
            )

            // Seed past few days of habit logs so streaks look realistic right away
            val logs = mutableListOf<HabitLogEntity>()
            for (offset in 0..4) {
                val d = today - offset
                logs.add(HabitLogEntity(habitId = h1, epochDay = d))
                if (offset != 2) logs.add(HabitLogEntity(habitId = h2, epochDay = d))
                if (offset in 1..3) logs.add(HabitLogEntity(habitId = h3, epochDay = d))
                if (offset in 0..2) logs.add(HabitLogEntity(habitId = h4, epochDay = d))
            }
            dao.insertHabitLogs(logs)

            dao.insertFocusSessions(
                listOf(
                    FocusSessionEntity(
                        taskId = task1Id,
                        taskTitle = "Finalize Q4 Android architecture & sprint roadmap",
                        durationMinutes = 25,
                        epochDay = today
                    ),
                    FocusSessionEntity(
                        taskId = null,
                        taskTitle = "Morning Deep Work Sprint",
                        durationMinutes = 25,
                        epochDay = today - 1
                    )
                )
            )

            dao.insertNotes(
                listOf(
                    NoteEntity(
                        title = "Q4 Product Launch & Architecture Notes",
                        content = "• Prioritize 100% offline Room DB speed\n• Keep Smart NLP Quick-Add under 50ms\n• Support direct 1-tap Edit & Delete on every card\n• Include full JSON Export/Restore vault",
                        projectId = 2,
                        projectName = "Work",
                        tagsCsv = "architecture,roadmap",
                        colorHex = 0xFF6366F1,
                        isPinned = true
                    ),
                    NoteEntity(
                        title = "Atomic Habits — Key Takeaways",
                        content = "1. Make it obvious\n2. Make it attractive\n3. Make it easy (2-minute rule)\n4. Make it satisfying (track streaks visually)",
                        projectId = 3,
                        projectName = "Personal",
                        tagsCsv = "books,mindset",
                        colorHex = 0xFF10B981,
                        isPinned = true
                    ),
                    NoteEntity(
                        title = "Grocery & Meal Prep Ideas",
                        content = "- High-protein overnight oats\n- Grilled salmon + quinoa bowls\n- Cold brew concentrate & oat milk",
                        projectId = 5,
                        projectName = "Shopping",
                        tagsCsv = "nutrition,food",
                        colorHex = 0xFFF59E0B,
                        isPinned = false
                    )
                )
            )
        }
    }
}
