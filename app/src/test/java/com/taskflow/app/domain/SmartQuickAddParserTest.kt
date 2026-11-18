package com.taskflow.app.domain

import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SmartQuickAddParserTest {

    private val sampleProjects = listOf(
        ProjectEntity(id = 1, name = "Inbox", colorHex = 0xFF6366F1, isDefault = true),
        ProjectEntity(id = 2, name = "Work", colorHex = 0xFFEC4899),
        ProjectEntity(id = 3, name = "Fitness", colorHex = 0xFFF59E0B)
    )

    @Test
    fun `parses priority, project, tags, time, tomorrow, recurrence, and pomodoros`() {
        val refDate = LocalDate.of(2026, 11, 18)
        val raw = "Submit quarterly report tomorrow at 5:30pm #Work @urgent @finance !p1 every week ~3p *"

        val parsed = SmartQuickAddParser.parse(raw, sampleProjects, refDate)

        assertEquals("Submit quarterly report", parsed.cleanTitle)
        assertEquals(TaskPriority.P1, parsed.priority)
        assertEquals(2L, parsed.projectId)
        assertEquals("Work", parsed.projectName)
        assertEquals(listOf("urgent", "finance"), parsed.tags)
        assertEquals(refDate.plusDays(1).toEpochDay(), parsed.dueDateEpochDay)
        assertEquals(17 * 60 + 30, parsed.dueTimeMinutes)
        assertEquals(RecurrenceType.WEEKLY, parsed.recurrenceType)
        assertEquals(3, parsed.estimatedPomodoros)
        assertTrue(parsed.isStarred)
    }

    @Test
    fun `parses custom day interval recurrence and default today date`() {
        val refDate = LocalDate.of(2026, 11, 18)
        val raw = "Water indoor plants every 3 days !p2 #Inbox"

        val parsed = SmartQuickAddParser.parse(raw, sampleProjects, refDate)

        assertEquals("Water indoor plants", parsed.cleanTitle)
        assertEquals(TaskPriority.P2, parsed.priority)
        assertEquals(RecurrenceType.CUSTOM_DAYS, parsed.recurrenceType)
        assertEquals(3, parsed.recurrenceIntervalDays)
        assertEquals(refDate.toEpochDay(), parsed.dueDateEpochDay)
    }
}
