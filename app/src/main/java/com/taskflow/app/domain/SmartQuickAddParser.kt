package com.taskflow.app.domain

import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.TaskPriority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class ParsedQuickAddResult(
    val cleanTitle: String,
    val priority: TaskPriority = TaskPriority.P4,
    val priorityExplicit: Boolean = false,
    val dueDateEpochDay: Long? = null,
    val dueDateLabel: String? = null,
    val dueTimeMinutes: Int? = null,
    val dueTimeLabel: String? = null,
    val projectName: String? = null,
    val projectId: Long? = null,
    val tags: List<String> = emptyList(),
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceIntervalDays: Int = 1,
    val estimatedPomodoros: Int = 1,
    val pomodoroExplicit: Boolean = false,
    val isStarred: Boolean = false
)

object SmartQuickAddParser {

    private val priorityRegex = Regex("""(?i)(?:^|\s)(!p1|!!1|!1|!urgent|!p2|!!2|!2|!important|!p3|!!3|!3|!p4|!!4|!4)(?=\s|$)""")
    private val projectRegex = Regex("""(?:^|\s)#([A-Za-z0-9_-]+)""")
    private val tagRegex = Regex("""(?:^|\s)@([A-Za-z0-9_-]+)""")
    private val pomoRegex = Regex("""(?i)(?:^|\s)~(\d+)\s*(?:p|pom|pomo|pomodoros?)(?=\s|$)""")
    private val starRegex = Regex("""(?i)(?:^|\s)(\*|!star|!starred)(?=\s|$)""")

    // Recurrence patterns
    private val everyNDaysRegex = Regex("""(?i)\bevery\s+(\d+)\s+days?\b""")
    private val dailyRegex = Regex("""(?i)\b(every\s+day|daily)\b""")
    private val weekdaysRegex = Regex("""(?i)\b(every\s+weekday|weekdays)\b""")
    private val weeklyRegex = Regex("""(?i)\b(every\s+week|weekly)\b""")
    private val monthlyRegex = Regex("""(?i)\b(every\s+month|monthly)\b""")

    // Date patterns
    private val inNDaysRegex = Regex("""(?i)\bin\s+(\d+)\s+days?\b""")
    private val isoDateRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
    private val todayRegex = Regex("""(?i)\b(today|tod|tonight)\b""")
    private val tomorrowRegex = Regex("""(?i)\b(tomorrow|tmr|tmrw)\b""")
    private val nextWeekRegex = Regex("""(?i)\b(next\s+week)\b""")
    private val weekdayRegex = Regex(
        """(?i)\b(?:next\s+)?(monday|mon|tuesday|tue|tues|wednesday|wed|thursday|thu|thur|thurs|friday|fri|saturday|sat|sunday|sun)\b"""
    )

    // Time patterns
    private val time12hRegex = Regex("""(?i)\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""")
    private val time24hRegex = Regex("""(?i)\b(?:at\s+)(\d{1,2}):(\d{2})\b""")
    private val namedTimeRegex = Regex("""(?i)\b(morning|noon|afternoon|evening|midnight)\b""")

    fun parse(
        rawInput: String,
        availableProjects: List<ProjectEntity> = emptyList(),
        referenceDate: LocalDate = LocalDate.now()
    ): ParsedQuickAddResult {
        if (rawInput.isBlank()) {
            return ParsedQuickAddResult(cleanTitle = "")
        }

        var working = " $rawInput "

        // 1. Priority
        var priority = TaskPriority.P4
        var priorityExplicit = false
        priorityRegex.find(working)?.let { match ->
            priorityExplicit = true
            val token = match.groupValues[1].lowercase()
            priority = when (token) {
                "!p1", "!!1", "!1", "!urgent" -> TaskPriority.P1
                "!p2", "!!2", "!2", "!important" -> TaskPriority.P2
                "!p3", "!!3", "!3" -> TaskPriority.P3
                else -> TaskPriority.P4
            }
            working = working.replaceRange(match.range, " ")
        }

        // 2. Project (#Work)
        var matchedProjectName: String? = null
        var matchedProjectId: Long? = null
        projectRegex.find(working)?.let { match ->
            val rawProj = match.groupValues[1]
            val found = availableProjects.find { it.name.equals(rawProj, ignoreCase = true) }
            matchedProjectName = found?.name ?: rawProj.replaceFirstChar { it.uppercase() }
            matchedProjectId = found?.id
            working = working.replaceRange(match.range, " ")
        }

        // 3. Tags (@urgent @deepwork)
        val tags = mutableListOf<String>()
        tagRegex.findAll(working).toList().asReversed().forEach { match ->
            tags.add(0, match.groupValues[1].lowercase())
            working = working.replaceRange(match.range, " ")
        }

        // 4. Pomodoro estimate (~3p)
        var estimatedPomodoros = 1
        var pomodoroExplicit = false
        pomoRegex.find(working)?.let { match ->
            estimatedPomodoros = (match.groupValues[1].toIntOrNull() ?: 1).coerceIn(1, 20)
            pomodoroExplicit = true
            working = working.replaceRange(match.range, " ")
        }

        // 5. Starred (* or !star)
        var isStarred = false
        starRegex.find(working)?.let { match ->
            isStarred = true
            working = working.replaceRange(match.range, " ")
        }

        // 6. Recurrence
        var recurrenceType = RecurrenceType.NONE
        var recurrenceIntervalDays = 1
        when {
            everyNDaysRegex.containsMatchIn(working) -> {
                val m = everyNDaysRegex.find(working)!!
                recurrenceType = RecurrenceType.CUSTOM_DAYS
                recurrenceIntervalDays = (m.groupValues[1].toIntOrNull() ?: 2).coerceAtLeast(1)
                working = working.replaceRange(m.range, " ")
            }
            weekdaysRegex.containsMatchIn(working) -> {
                val m = weekdaysRegex.find(working)!!
                recurrenceType = RecurrenceType.WEEKDAYS
                working = working.replaceRange(m.range, " ")
            }
            dailyRegex.containsMatchIn(working) -> {
                val m = dailyRegex.find(working)!!
                recurrenceType = RecurrenceType.DAILY
                working = working.replaceRange(m.range, " ")
            }
            weeklyRegex.containsMatchIn(working) -> {
                val m = weeklyRegex.find(working)!!
                recurrenceType = RecurrenceType.WEEKLY
                working = working.replaceRange(m.range, " ")
            }
            monthlyRegex.containsMatchIn(working) -> {
                val m = monthlyRegex.find(working)!!
                recurrenceType = RecurrenceType.MONTHLY
                working = working.replaceRange(m.range, " ")
            }
        }

        // 7. Time
        var dueTimeMinutes: Int? = null
        var dueTimeLabel: String? = null
        var impliedTonight = false

        if ( working.contains(Regex("""(?i)\btonight\b""")) ) {
            impliedTonight = true
        }

        time12hRegex.find(working)?.let { m ->
            val hourRaw = m.groupValues[1].toIntOrNull() ?: 9
            val minuteRaw = m.groupValues[2].toIntOrNull() ?: 0
            val amPm = m.groupValues[3].lowercase()
            if (hourRaw in 1..12 && minuteRaw in 0..59) {
                val hour24 = when {
                    amPm == "am" && hourRaw == 12 -> 0
                    amPm == "pm" && hourRaw < 12 -> hourRaw + 12
                    else -> hourRaw
                }
                dueTimeMinutes = hour24 * 60 + minuteRaw
                dueTimeLabel = formatMinutes(dueTimeMinutes!!)
                working = working.replaceRange(m.range, " ")
            }
        } ?: time24hRegex.find(working)?.let { m ->
            val hour24 = m.groupValues[1].toIntOrNull() ?: 9
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            if (hour24 in 0..23 && minute in 0..59) {
                dueTimeMinutes = hour24 * 60 + minute
                dueTimeLabel = formatMinutes(dueTimeMinutes!!)
                working = working.replaceRange(m.range, " ")
            }
        } ?: namedTimeRegex.find(working)?.let { m ->
            val token = m.groupValues[1].lowercase()
            dueTimeMinutes = when (token) {
                "morning" -> 9 * 60
                "noon" -> 12 * 60
                "afternoon" -> 14 * 60
                "evening" -> 18 * 60
                "midnight" -> 23 * 60 + 59
                else -> 9 * 60
            }
            dueTimeLabel = formatMinutes(dueTimeMinutes!!)
            working = working.replaceRange(m.range, " ")
        }

        if (impliedTonight && dueTimeMinutes == null) {
            dueTimeMinutes = 20 * 60
            dueTimeLabel = "8:00 PM"
        }

        // 8. Date
        var dueDate: LocalDate? = null
        var dueDateLabel: String? = null

        when {
            isoDateRegex.containsMatchIn(working) -> {
                val m = isoDateRegex.find(working)!!
                runCatching { LocalDate.parse(m.groupValues[1]) }.getOrNull()?.let { parsed ->
                    dueDate = parsed
                    dueDateLabel = parsed.toString()
                    working = working.replaceRange(m.range, " ")
                }
            }
            inNDaysRegex.containsMatchIn(working) -> {
                val m = inNDaysRegex.find(working)!!
                val days = (m.groupValues[1].toIntOrNull() ?: 1).coerceAtLeast(1)
                dueDate = referenceDate.plusDays(days.toLong())
                dueDateLabel = "In $days days"
                working = working.replaceRange(m.range, " ")
            }
            tomorrowRegex.containsMatchIn(working) -> {
                val m = tomorrowRegex.find(working)!!
                dueDate = referenceDate.plusDays(1)
                dueDateLabel = "Tomorrow"
                working = working.replaceRange(m.range, " ")
            }
            todayRegex.containsMatchIn(working) -> {
                val m = todayRegex.find(working)!!
                dueDate = referenceDate
                dueDateLabel = "Today"
                working = working.replaceRange(m.range, " ")
            }
            nextWeekRegex.containsMatchIn(working) -> {
                val m = nextWeekRegex.find(working)!!
                dueDate = referenceDate.plusWeeks(1)
                dueDateLabel = "Next Week"
                working = working.replaceRange(m.range, " ")
            }
            weekdayRegex.containsMatchIn(working) -> {
                val m = weekdayRegex.find(working)!!
                val dayToken = m.groupValues[1].lowercase()
                val targetDow = parseDayOfWeek(dayToken)
                if (targetDow != null) {
                    dueDate = referenceDate.with(TemporalAdjusters.nextOrSame(targetDow)).let {
                        if (it == referenceDate) it.plusWeeks(1) else it
                    }
                    dueDateLabel = targetDow.name.lowercase().replaceFirstChar { it.uppercase() }
                    working = working.replaceRange(m.range, " ")
                }
            }
        }

        // If recurrence or time was specified without a date, default dueDate to Today
        if (dueDate == null && (recurrenceType != RecurrenceType.NONE || dueTimeMinutes != null)) {
            dueDate = referenceDate
            dueDateLabel = "Today"
        }

        val cleanedTitle = working.replace(Regex("""\s+"""), " ").trim()
            .ifEmpty { rawInput.trim() }

        return ParsedQuickAddResult(
            cleanTitle = cleanedTitle,
            priority = priority,
            priorityExplicit = priorityExplicit,
            dueDateEpochDay = dueDate?.toEpochDay(),
            dueDateLabel = dueDateLabel,
            dueTimeMinutes = dueTimeMinutes,
            dueTimeLabel = dueTimeLabel,
            projectName = matchedProjectName,
            projectId = matchedProjectId,
            tags = tags.distinct(),
            recurrenceType = recurrenceType,
            recurrenceIntervalDays = recurrenceIntervalDays,
            estimatedPomodoros = estimatedPomodoros,
            pomodoroExplicit = pomodoroExplicit,
            isStarred = isStarred
        )
    }

    private fun parseDayOfWeek(token: String): DayOfWeek? = when {
        token.startsWith("mon") -> DayOfWeek.MONDAY
        token.startsWith("tue") -> DayOfWeek.TUESDAY
        token.startsWith("wed") -> DayOfWeek.WEDNESDAY
        token.startsWith("thu") -> DayOfWeek.THURSDAY
        token.startsWith("fri") -> DayOfWeek.FRIDAY
        token.startsWith("sat") -> DayOfWeek.SATURDAY
        token.startsWith("sun") -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        val amPm = if (h >= 12) "PM" else "AM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "%d:%02d %s".format(h12, m, amPm)
    }
}
