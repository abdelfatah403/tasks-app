package com.taskflow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.model.ProjectEntity
import com.taskflow.app.data.model.RecurrenceType
import com.taskflow.app.data.model.SubtaskEntity
import com.taskflow.app.data.model.TaskEntity
import com.taskflow.app.data.model.TaskPriority
import com.taskflow.app.ui.TaskFilterMode
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    allTasks: List<TaskEntity>,
    subtasks: List<SubtaskEntity>,
    projects: List<ProjectEntity>,
    selectedFilter: TaskFilterMode,
    selectedProjectId: Long?,
    selectedTag: String?,
    searchQuery: String,
    onSelectFilter: (TaskFilterMode) -> Unit,
    onSelectProject: (Long?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onToggleStar: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit = {},
    onDeleteTask: (TaskEntity) -> Unit,
    onChangePriority: (TaskEntity, TaskPriority) -> Unit,
    onRescheduleTask: (TaskEntity, Long) -> Unit,
    onAddSubtask: (Long, String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onDeleteSubtask: (SubtaskEntity) -> Unit,
    onStartFocusForTask: (TaskEntity) -> Unit,
    onOpenVoiceQuickAdd: () -> Unit,
    onClearCompleted: () -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val completedTodayCount = allTasks.count { it.isCompleted }
    val totalTodayCount = allTasks.count {
        it.dueDateEpochDay == null || it.dueDateEpochDay <= todayEpoch || it.isCompleted
    }.coerceAtLeast(1)
    val dailyGoalProgress = (completedTodayCount.toFloat() / totalTodayCount.toFloat()).coerceIn(0f, 1f)

    // Collect all unique tags across tasks
    val allTags = remember(allTasks) {
        allTasks.flatMap { it.tagsList }.distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Todoist Karma + Daily Goal Progress Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Daily Momentum • $completedTodayCount / $totalTodayCount Done",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Karma Score: ${120 + completedTodayCount * 15} pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onOpenVoiceQuickAdd() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Quick Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Voice Add",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { dailyGoalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Color(0xFF10B981),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 2. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search tasks, #projects, or @tags...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. View Filter Tabs (Today, Upcoming, Starred, All, Completed)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedFilter == mode,
                        onClick = { onSelectFilter(mode) },
                        label = {
                            Text(
                                text = mode.label,
                                fontWeight = if (selectedFilter == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // 4. Project & Tag Filter Row (Starts with "All" so all projects & tags show by default)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllCategoriesAndTags = selectedProjectId == null && selectedTag == null
                FilterChip(
                    selected = isAllCategoriesAndTags,
                    onClick = {
                        onSelectProject(null)
                        onSelectTag(null)
                    },
                    label = {
                        Text(
                            text = "All",
                            fontWeight = if (isAllCategoriesAndTags) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )

                projects.forEach { project ->
                    val isSelected = selectedProjectId == project.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectProject(project.id) },
                        label = { Text("#${project.name}") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(project.colorHex))
                            )
                        }
                    )
                }

                allTags.forEach { tag ->
                    val isSelected = selectedTag.equals(tag, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTag(tag) },
                        label = { Text("@$tag") }
                    )
                }
            }
        }

        if (selectedFilter == TaskFilterMode.COMPLETED && tasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClearCompleted) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear all completed")
                    }
                }
            }
        }

        // 5. Task List Items
        if (tasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "All clear in ${selectedFilter.label}!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap + below or use Voice Add to capture your next task.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                val taskSubtasks = subtasks.filter { it.taskId == task.id }
                val projectColor = projects.find { it.id == task.projectId }?.colorHex ?: 0xFF6366F1
                TaskCardItem(
                    task = task,
                    subtasks = taskSubtasks,
                    projectColorHex = projectColor,
                    todayEpoch = todayEpoch,
                    onToggleComplete = { onToggleComplete(task) },
                    onToggleStar = { onToggleStar(task) },
                    onEditTask = { onEditTask(task) },
                    onDeleteTask = { onDeleteTask(task) },
                    onChangePriority = { newPrio -> onChangePriority(task, newPrio) },
                    onRescheduleTask = { epoch -> onRescheduleTask(task, epoch) },
                    onAddSubtask = { title -> onAddSubtask(task.id, title) },
                    onToggleSubtask = onToggleSubtask,
                    onDeleteSubtask = onDeleteSubtask,
                    onStartFocus = { onStartFocusForTask(task) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TaskCardItem(
    task: TaskEntity,
    subtasks: List<SubtaskEntity>,
    projectColorHex: Long,
    todayEpoch: Long,
    onToggleComplete: () -> Unit,
    onToggleStar: () -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onChangePriority: (TaskPriority) -> Unit,
    onRescheduleTask: (Long) -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onDeleteSubtask: (SubtaskEntity) -> Unit,
    onStartFocus: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }

    val priorityColor = Color(task.priority.colorHex)
    val isOverdue = !task.isCompleted && task.dueDateEpochDay != null && task.dueDateEpochDay < todayEpoch

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            when (targetValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe Left -> Delete immediately (whether completed or not!)
                    onDeleteTask()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe Right -> Toggle completion
                    onToggleComplete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981).copy(alpha = 0.25f)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444).copy(alpha = 0.25f)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Complete", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { expanded = !expanded }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Priority-colored Complete Button
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete task",
                            tint = if (task.isCompleted) Color(0xFF10B981) else priorityColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )

                        if (task.notes.isNotBlank()) {
                            Text(
                                text = task.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expanded) 5 else 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Metadata Badges Row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Priority Pill
                            MetaPill(
                                text = task.priority.shortTag,
                                color = priorityColor
                            )

                            // Project Pill
                            MetaPill(
                                text = "#${task.projectName}",
                                color = Color(projectColorHex)
                            )

                            // Due Date + Time Pill
                            task.dueDateEpochDay?.let { epoch ->
                                val dateLabel = when (epoch) {
                                    todayEpoch -> "Today"
                                    todayEpoch + 1 -> "Tomorrow"
                                    todayEpoch - 1 -> "Yesterday"
                                    else -> LocalDate.ofEpochDay(epoch).toString()
                                }
                                val fullDateStr = if (task.formattedTime != null) "$dateLabel • ${task.formattedTime}" else dateLabel
                                MetaPill(
                                    text = if (isOverdue) "⚠️ Overdue ($fullDateStr)" else "📅 $fullDateStr",
                                    color = if (isOverdue) Color(0xFFEF4444) else Color(0xFF3B82F6)
                                )
                            }

                            // Recurrence Badge
                            if (task.recurrence != RecurrenceType.NONE) {
                                MetaPill(
                                    text = "🔁 ${task.recurrence.label}",
                                    color = Color(0xFF10B981)
                                )
                            }

                            // Pomodoro Badge
                            MetaPill(
                                text = "🍅 ${task.completedPomodoros}/${task.estimatedPomodoros}",
                                color = Color(0xFFF59E0B)
                            )

                            // Subtasks count badge
                            if (subtasks.isNotEmpty()) {
                                val doneSub = subtasks.count { it.isCompleted }
                                MetaPill(
                                    text = "☑️ $doneSub/${subtasks.size}",
                                    color = Color(0xFF8B5CF6)
                                )
                            }

                            // Tags
                            task.tagsList.forEach { tag ->
                                MetaPill(
                                    text = "@$tag",
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Right-side Quick Actions: Star + Edit + Delete + Start Focus
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleStar,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star",
                                    tint = if (task.isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Direct 1-Tap Edit / Update Button
                            IconButton(
                                onClick = onEditTask,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Task",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Direct 1-Tap Delete Button (works anytime without completing the task)
                            IconButton(
                                onClick = onDeleteTask,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Task",
                                    tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!task.isCompleted) {
                                IconButton(
                                    onClick = onStartFocus,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Start Focus Timer",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Expandable Task Detail Drawer (Subtasks, Quick Reschedule, Quadrant Switch, Delete)
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtasks Checklist
                        Text(
                            text = "Checklist / Subtasks",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

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
                                IconButton(onClick = { onDeleteSubtask(sub) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
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
                                value = newSubtaskTitle,
                                onValueChange = { newSubtaskTitle = it },
                                placeholder = { Text("Add subtask step...") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (newSubtaskTitle.isNotBlank()) {
                                        onAddSubtask(newSubtaskTitle)
                                        newSubtaskTitle = ""
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add subtask")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Priority Switcher + Reschedule + Delete Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TaskPriority.entries.forEach { prio ->
                                FilterChip(
                                    selected = task.priority == prio,
                                    onClick = { onChangePriority(prio) },
                                    label = { Text(prio.shortTag, fontSize = 11.sp) }
                                )
                            }

                            FilterChip(
                                selected = false,
                                onClick = { onRescheduleTask(todayEpoch) },
                                label = { Text("Today", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = { onRescheduleTask(todayEpoch + 1) },
                                label = { Text("+1 Day", fontSize = 11.sp) }
                            )

                            TextButton(onClick = onEditTask) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Edit", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }

                            TextButton(onClick = onDeleteTask) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
