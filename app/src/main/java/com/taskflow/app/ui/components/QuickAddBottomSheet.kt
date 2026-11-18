package com.taskflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.TaskPriority
import com.taskflow.app.domain.SmartQuickAddParser
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddBottomSheet(
    projects: List<ProjectEntity>,
    initialText: String = "",
    initialPriority: TaskPriority? = null,
    onRequestVoiceInput: ((String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        rawInput: String,
        notes: String,
        overridePriority: TaskPriority?,
        overrideProjectId: Long?,
        overrideEpochDay: Long?,
        overrideTimeMinutes: Int?,
        overrideRecurrence: RecurrenceType?,
        overrideStarred: Boolean?,
        subtasks: List<String>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var rawTitle by remember(initialText) { mutableStateOf(initialText) }
    var notes by remember { mutableStateOf("") }
    var manualPriority by remember(initialPriority) { mutableStateOf(initialPriority) }
    var manualProjectId by remember { mutableStateOf<Long?>(null) }
    var manualEpochDay by remember { mutableStateOf<Long?>(null) }
    var manualRecurrence by remember { mutableStateOf<RecurrenceType?>(null) }
    var manualStarred by remember { mutableStateOf<Boolean?>(null) }

    var subtaskDraft by remember { mutableStateOf("") }
    val subtaskList = remember { mutableStateListOf<String>() }

    val parsed = remember(rawTitle, projects) {
        SmartQuickAddParser.parse(rawTitle, projects)
    }

    val effectivePriority = manualPriority ?: parsed.priority
    val effectiveProject = manualProjectId?.let { id -> projects.find { it.id == id } }
        ?: parsed.projectId?.let { id -> projects.find { it.id == id } }
        ?: projects.firstOrNull()
    val effectiveRecurrence = manualRecurrence ?: parsed.recurrenceType
    val effectiveStarred = manualStarred ?: parsed.isStarred
    val todayEpoch = LocalDate.now().toEpochDay()
    val effectiveEpochDay = manualEpochDay ?: parsed.dueDateEpochDay ?: todayEpoch

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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Quick-Add",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Input Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable {
                            onRequestVoiceInput { spokenText ->
                                rawTitle = if (rawTitle.isBlank()) spokenText else "$rawTitle $spokenText"
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(onClick = {
                        manualStarred = !effectiveStarred
                    }) {
                        Icon(
                            imageVector = if (effectiveStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star task",
                            tint = if (effectiveStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "Try typing: \"Submit report tomorrow 5pm #Work @urgent !p1 every week ~2p\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            OutlinedTextField(
                value = rawTitle,
                onValueChange = { rawTitle = it },
                placeholder = { Text("What needs to be done?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = false,
                maxLines = 3
            )

            // Live NLP Recognized Token Preview Pills
            if (rawTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ParsedBadge(
                        text = "✨ Title: ${parsed.cleanTitle.ifBlank { rawTitle }}",
                        bgColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    ParsedBadge(
                        text = "🚩 ${effectivePriority.shortTag} (${effectivePriority.quadrantTitle})",
                        bgColor = Color(effectivePriority.colorHex).copy(alpha = 0.22f)
                    )
                    val dateLabel = when (effectiveEpochDay) {
                        todayEpoch -> "Today"
                        todayEpoch + 1 -> "Tomorrow"
                        else -> LocalDate.ofEpochDay(effectiveEpochDay).toString()
                    }
                    ParsedBadge(
                        text = "📅 $dateLabel" + (parsed.dueTimeLabel?.let { " • $it" } ?: ""),
                        bgColor = Color(0xFF3B82F6).copy(alpha = 0.2f)
                    )
                    effectiveProject?.let { proj ->
                        ParsedBadge(
                            text = "📁 #${proj.name}",
                            bgColor = Color(proj.colorHex).copy(alpha = 0.22f)
                        )
                    }
                    if (effectiveRecurrence != RecurrenceType.NONE) {
                        ParsedBadge(
                            text = "🔁 ${effectiveRecurrence.label}",
                            bgColor = Color(0xFF10B981).copy(alpha = 0.22f)
                        )
                    }
                    parsed.tags.forEach { tag ->
                        ParsedBadge(
                            text = "🏷️ @$tag",
                            bgColor = Color(0xFF8B5CF6).copy(alpha = 0.22f)
                        )
                    }
                    if (parsed.pomodoroExplicit || parsed.estimatedPomodoros > 1) {
                        ParsedBadge(
                            text = "🍅 ${parsed.estimatedPomodoros} Pomodoros",
                            bgColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Notes or description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Date Selector Row
            Text(
                text = "Schedule Date",
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
                val dateOptions = listOf(
                    "Today" to todayEpoch,
                    "Tomorrow" to (todayEpoch + 1),
                    "In 3 Days" to (todayEpoch + 3),
                    "Next Week" to (todayEpoch + 7)
                )
                dateOptions.forEach { (label, epoch) ->
                    FilterChip(
                        selected = effectiveEpochDay == epoch,
                        onClick = { manualEpochDay = epoch },
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

            // Priority / Eisenhower Quadrant Selector
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
                        selected = effectivePriority == prio,
                        onClick = { manualPriority = prio },
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

            // Project Selector Row
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
                        selected = effectiveProject?.id == project.id,
                        onClick = { manualProjectId = project.id },
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

            // Recurrence Selector Row
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
                        selected = effectiveRecurrence == rec,
                        onClick = { manualRecurrence = rec },
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

            // Subtasks Checklist Builder
            Text(
                text = "Subtasks / Checklist Steps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            subtaskList.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { subtaskList.removeAt(index) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove subtask",
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
                    value = subtaskDraft,
                    onValueChange = { subtaskDraft = it },
                    placeholder = { Text("Add a step...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (subtaskDraft.isNotBlank()) {
                            subtaskList.add(subtaskDraft.trim())
                            subtaskDraft = ""
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add step")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (subtaskDraft.isNotBlank()) {
                        subtaskList.add(subtaskDraft.trim())
                    }
                    onSubmit(
                        rawTitle,
                        notes,
                        manualPriority,
                        manualProjectId,
                        manualEpochDay,
                        null,
                        manualRecurrence,
                        manualStarred,
                        subtaskList.toList()
                    )
                    onDismiss()
                },
                enabled = rawTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Task", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ParsedBadge(text: String, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
