package com.taskflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.TaskEntity
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarTimelineScreen(
    allTasks: List<TaskEntity>,
    selectedEpochDay: Long,
    onSelectEpochDay: (Long) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onOpenQuickAddForSelectedDay: () -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    // 14-day rolling strip (-2 days to +11 days)
    val dayStrip = remember(todayEpoch) {
        (-2L..11L).map { offset -> LocalDate.ofEpochDay(todayEpoch + offset) }
    }

    val tasksForSelectedDate = remember(allTasks, selectedEpochDay) {
        allTasks.filter { task ->
            task.dueDateEpochDay == selectedEpochDay ||
                (task.recurrence == RecurrenceType.DAILY && (task.dueDateEpochDay ?: todayEpoch) <= selectedEpochDay && !task.isCompleted)
        }.sortedWith(
            compareBy<TaskEntity> { it.isCompleted }
                .thenBy { it.dueTimeMinutes ?: 9999 }
                .thenBy { it.priorityCode }
        )
    }

    val timedTasks = tasksForSelectedDate.filter { it.dueTimeMinutes != null }
    val allDayTasks = tasksForSelectedDate.filter { it.dueTimeMinutes == null }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Header + Jump to Today
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedDate.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedEpochDay == todayEpoch) "Today's Schedule & Timeline"
                        else selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedEpochDay != todayEpoch) {
                TextButton(onClick = { onSelectEpochDay(todayEpoch) }) {
                    Text("Jump to Today")
                }
            }
        }

        // Horizontal 14-day Date Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dayStrip, key = { it.toEpochDay() }) { date ->
                val epoch = date.toEpochDay()
                val isSelected = epoch == selectedEpochDay
                val isToday = epoch == todayEpoch
                val taskCount = allTasks.count { !it.isCompleted && it.dueDateEpochDay == epoch }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .width(56.dp)
                        .clickable { onSelectEpochDay(epoch) }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        taskCount == 0 -> Color.Transparent
                                        isSelected -> Color.White
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Timed Timeline Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time-Blocked Timeline (${timedTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onOpenQuickAddForSelectedDay) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule Task")
                    }
                }
            }

            if (timedTasks.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No time-blocked tasks for this day. Add a time like \"at 3pm\" in Quick-Add!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(timedTasks, key = { "timed_${it.id}" }) { task ->
                    TimelineTaskRow(task = task, onToggleComplete = { onToggleComplete(task) })
                }
            }

            // All-Day & Flexible Tasks Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All-Day & Flexible Tasks (${allDayTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (allDayTasks.isEmpty()) {
                item {
                    Text(
                        text = "No unscheduled all-day tasks for this date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(allDayTasks, key = { "allday_${it.id}" }) { task ->
                    TimelineTaskRow(task = task, onToggleComplete = { onToggleComplete(task) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

@Composable
private fun TimelineTaskRow(
    task: TaskEntity,
    onToggleComplete: () -> Unit
) {
    val prioColor = Color(task.priority.colorHex)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Time Column
        Column(
            modifier = Modifier.width(68.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = task.formattedTime ?: "All Day",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (task.recurrence != RecurrenceType.NONE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = task.recurrence.label.take(7),
                        fontSize = 9.sp,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Timeline Connector Dot & Line
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(prioColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Task Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, prioColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.isCompleted) Color(0xFF10B981) else prioColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "#${task.projectName} • ${task.priority.shortTag} • 🍅 ${task.estimatedPomodoros}p",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
