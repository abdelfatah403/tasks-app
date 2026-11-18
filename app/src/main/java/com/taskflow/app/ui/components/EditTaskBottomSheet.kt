package com.taskflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TaskEntity
import com.taskflow.app.data.model.TaskPriority
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskBottomSheet(
    task: TaskEntity,
    projects: List<ProjectEntity>,
    subtasks: List<SubtaskEntity>,
    onDismiss: () -> Unit,
    onSaveTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddSubtask: (Long, String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onDeleteSubtask: (SubtaskEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val todayEpoch = LocalDate.now().toEpochDay()

    var title by remember(task.id) { mutableStateOf(task.title) }
    var notes by remember(task.id) { mutableStateOf(task.notes) }
    var selectedPriority by remember(task.id) { mutableStateOf(task.priority) }
    var selectedProjectId by remember(task.id) { mutableStateOf(task.projectId) }
    var selectedEpochDay by remember(task.id) { mutableStateOf(task.dueDateEpochDay) }
    var selectedTimeMinutes by remember(task.id) { mutableStateOf(task.dueTimeMinutes) }
    var selectedRecurrence by remember(task.id) { mutableStateOf(task.recurrence) }
    var tagsInput by remember(task.id) { mutableStateOf(task.tagsCsv) }
    var estimatedPomodoros by remember(task.id) { mutableIntStateOf(task.estimatedPomodoros.coerceAtLeast(1)) }
    var isStarred by remember(task.id) { mutableStateOf(task.isStarred) }

    var newSubtaskDraft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit & Update Task",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isStarred = !isStarred }) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star task",
                            tint = if (isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            onDeleteTask(task)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Task",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Title & Notes
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Due Date Selector
            val currentDayStr = selectedEpochDay?.let { epoch ->
                when (epoch) {
                    todayEpoch -> "Today (${LocalDate.ofEpochDay(epoch)})"
                    todayEpoch + 1 -> "Tomorrow (${LocalDate.ofEpochDay(epoch)})"
                    else -> LocalDate.ofEpochDay(epoch).toString()
                }
            } ?: "No Due Date"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due Date: $currentDayStr",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            selectedEpochDay = (selectedEpochDay ?: todayEpoch) - 1
                        }
                    ) {
                        Text("-1d", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = {
                            selectedEpochDay = (selectedEpochDay ?: todayEpoch) + 1
                        }
                    ) {
                        Text("+1d", fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateChips = listOf(
                    "Today" to todayEpoch,
                    "Tomorrow" to (todayEpoch + 1),
                    "In 3 Days" to (todayEpoch + 3),
                    "Next Week" to (todayEpoch + 7),
                    "No Date" to null
                )
                dateChips.forEach { (label, epoch) ->
                    FilterChip(
                        selected = selectedEpochDay == epoch,
                        onClick = { selectedEpochDay = epoch },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Due Time Selector
            Text(
                text = "Due Time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timePresets = listOf(
                    "All Day" to null,
                    "9:00 AM" to (9 * 60),
                    "12:00 PM" to (12 * 60),
                    "2:00 PM" to (14 * 60),
                    "5:00 PM" to (17 * 60),
                    "8:00 PM" to (20 * 60)
                )
                timePresets.forEach { (label, mins) ->
                    FilterChip(
                        selected = selectedTimeMinutes == mins,
                        onClick = { selectedTimeMinutes = mins },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            // 4. Priority & Eisenhower Quadrant
            Text(
                text = "Priority & Eisenhower Quadrant",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { prio ->
                    FilterChip(
                        selected = selectedPriority == prio,
                        onClick = { selectedPriority = prio },
                        label = { Text("${prio.shortTag}: ${prio.quadrantTitle}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = Color(prio.colorHex),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    )
                }
            }

            // 5. Project / List Selector
            Text(
                text = "Project / List",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projects.forEach { project ->
                    FilterChip(
                        selected = selectedProjectId == project.id,
                        onClick = { selectedProjectId = project.id },
                        label = { Text(project.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(project.colorHex))
                            )
                        }
                    )
                }
            }

            // 6. Recurrence Selector
            Text(
                text = "Repeat / Recurrence",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RecurrenceType.NONE,
                    RecurrenceType.DAILY,
                    RecurrenceType.WEEKDAYS,
                    RecurrenceType.WEEKLY,
                    RecurrenceType.MONTHLY
                ).forEach { rec ->
                    FilterChip(
                        selected = selectedRecurrence == rec,
                        onClick = { selectedRecurrence = rec },
                        label = { Text(rec.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            // 7. Tags & Pomodoro Target Row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma-separated)") },
                    placeholder = { Text("urgent, deepwork") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🍅 Target",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (estimatedPomodoros > 1) estimatedPomodoros-- },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "${estimatedPomodoros}p",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { if (estimatedPomodoros < 20) estimatedPomodoros++ },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }

            // 8. Subtasks Checklist Editor
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Subtasks / Checklist (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            subtasks.forEach { sub ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sub.isCompleted,
                        onCheckedChange = { onToggleSubtask(sub) }
                    )
                    Text(
                        text = sub.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDeleteSubtask(sub) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Delete subtask",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskDraft,
                    onValueChange = { newSubtaskDraft = it },
                    placeholder = { Text("Add a new subtask step...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskDraft.isNotBlank()) {
                            onAddSubtask(task.id, newSubtaskDraft.trim())
                            newSubtaskDraft = ""
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subtask")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Save & Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (newSubtaskDraft.isNotBlank()) {
                            onAddSubtask(task.id, newSubtaskDraft.trim())
                        }
                        val matchedProject = projects.find { it.id == selectedProjectId }
                        val cleanedTags = tagsInput
                            .split(",")
                            .map { it.trim().removePrefix("@") }
                            .filter { it.isNotEmpty() }
                            .joinToString(",")

                        val updated = task.copy(
                            title = title.trim().ifBlank { task.title },
                            notes = notes.trim(),
                            projectId = matchedProject?.id ?: task.projectId,
                            projectName = matchedProject?.name ?: task.projectName,
                            priorityCode = selectedPriority.code,
                            dueDateEpochDay = selectedEpochDay,
                            dueTimeMinutes = selectedTimeMinutes,
                            isStarred = isStarred,
                            recurrenceType = selectedRecurrence.name,
                            tagsCsv = cleanedTags,
                            estimatedPomodoros = estimatedPomodoros
                        )
                        onSaveTask(updated)
                        onDismiss()
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
