package com.taskflow.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.taskflow.app.data.model.FocusSessionEntity
import com.taskflow.app.data.model.HabitEntity
import com.taskflow.app.data.model.HabitLogEntity
import com.taskflow.app.data.model.NoteEntity
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TagEntity
import com.taskflow.app.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskFlowDao {

    // --- Projects ---
    @Query("SELECT * FROM projects ORDER BY isDefault DESC, name ASC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsSnapshot(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("UPDATE tasks SET projectName = :newName WHERE projectId = :projectId")
    suspend fun updateTaskProjectNames(projectId: Long, newName: String)

    @Query("UPDATE notes SET projectName = :newName WHERE projectId = :projectId")
    suspend fun updateNoteProjectNames(projectId: Long, newName: String)

    @Query("UPDATE tasks SET projectId = 1, projectName = 'Inbox' WHERE projectId = :deletedProjectId")
    suspend fun reassignTasksToInbox(deletedProjectId: Long)

    @Query("UPDATE notes SET projectId = 1, projectName = 'Inbox' WHERE projectId = :deletedProjectId")
    suspend fun reassignNotesToInbox(deletedProjectId: Long)

    // --- Tags ---
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsSnapshot(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE LOWER(name) = LOWER(:tagName)")
    suspend fun deleteTagByName(tagName: String)

    // --- Tasks ---
    @Query(
        """
        SELECT * FROM tasks 
        ORDER BY isCompleted ASC, 
                 priorityCode ASC, 
                 CASE WHEN dueDateEpochDay IS NULL THEN 1 ELSE 0 END ASC, 
                 dueDateEpochDay ASC, 
                 CASE WHEN dueTimeMinutes IS NULL THEN 1 ELSE 0 END ASC,
                 dueTimeMinutes ASC,
                 createdAtMillis DESC
        """
    )
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priorityCode ASC, dueDateEpochDay ASC LIMIT 8")
    suspend fun getTopActiveTasksForWidget(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSnapshot(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun clearCompletedTasks()

    // --- Subtasks ---
    @Query("SELECT * FROM subtasks ORDER BY id ASC")
    fun observeAllSubtasks(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasksSnapshot(): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)

    // --- Habits ---
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsSnapshot(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habit_logs")
    fun observeAllHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllHabitLogsSnapshot(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getHabitLog(habitId: Long, epochDay: Long): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLogs(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteHabitLog(habitId: Long, epochDay: Long)

    // --- Focus Sessions ---
    @Query("SELECT * FROM focus_sessions ORDER BY completedAtMillis DESC")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions")
    suspend fun getAllFocusSessionsSnapshot(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSessionEntity>)

    // --- Notes ---
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSnapshot(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // --- Full Wipe & Restore for Offline JSON Backup ---
    @Query("DELETE FROM projects")
    suspend fun clearAllProjects()

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("DELETE FROM subtasks")
    suspend fun clearAllSubtasks()

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllHabitLogs()

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllFocusSessions()

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    @Query("DELETE FROM tags")
    suspend fun clearAllTags()

    @Transaction
    suspend fun restoreFullBackup(
        projects: List<ProjectEntity>,
        tags: List<TagEntity>,
        tasks: List<TaskEntity>,
        subtasks: List<SubtaskEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        focusSessions: List<FocusSessionEntity>,
        notes: List<NoteEntity>
    ) {
        clearAllSubtasks()
        clearAllTasks()
        clearAllProjects()
        clearAllTags()
        clearAllHabitLogs()
        clearAllHabits()
        clearAllFocusSessions()
        clearAllNotes()

        insertProjects(projects)
        insertTags(tags)
        insertTasks(tasks)
        insertSubtasks(subtasks)
        insertHabits(habits)
        insertHabitLogs(habitLogs)
        insertFocusSessions(focusSessions)
        insertNotes(notes)
    }
}
