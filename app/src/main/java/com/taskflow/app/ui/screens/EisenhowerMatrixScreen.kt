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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.model.TaskEntity
import com.taskflow.app.data.model.TaskPriority

@Composable
fun EisenhowerMatrixScreen(
    allTasks: List<TaskEntity>,
    onToggleComplete: (TaskEntity) -> Unit,
    onMovePriority: (TaskEntity, TaskPriority) -> Unit,
    onEditTask: (TaskEntity) -> Unit = {},
    onDeleteTask: (TaskEntity) -> Unit = {},
    onQuickAddInQuadrant: (TaskPriority) -> Unit
) {
    val activeTasks = remember(allTasks) { allTasks.filter { !it.isCompleted } }

    val q1Tasks = activeTasks.filter { it.priority == TaskPriority.P1 }
    val q2Tasks = activeTasks.filter { it.priority == TaskPriority.P2 }
    val q3Tasks = activeTasks.filter { it.priority == TaskPriority.P3 }
    val q4Tasks = activeTasks.filter { it.priority == TaskPriority.P4 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Eisenhower Priority Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Focus on Q1 (Do First) & Q2 (Schedule). Tap arrows to move tasks across quadrants.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Top Row: Q1 & Q2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuadrantCard(
                    priority = TaskPriority.P1,
                    tasks = q1Tasks,
                    onToggleComplete = onToggleComplete,
                    onMovePriority = onMovePriority,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask,
                    onQuickAdd = { onQuickAddInQuadrant(TaskPriority.P1) },
                    modifier = Modifier.weight(1f)
                )
                QuadrantCard(
                    priority = TaskPriority.P2,
                    tasks = q2Tasks,
                    onToggleComplete = onToggleComplete,
                    onMovePriority = onMovePriority,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask,
                    onQuickAdd = { onQuickAddInQuadrant(TaskPriority.P2) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom Row: Q3 & Q4
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuadrantCard(
                    priority = TaskPriority.P3,
                    tasks = q3Tasks,
                    onToggleComplete = onToggleComplete,
                    onMovePriority = onMovePriority,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask,
                    onQuickAdd = { onQuickAddInQuadrant(TaskPriority.P3) },
                    modifier = Modifier.weight(1f)
                )
                QuadrantCard(
                    priority = TaskPriority.P4,
                    tasks = q4Tasks,
                    onToggleComplete = onToggleComplete,
                    onMovePriority = onMovePriority,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask,
                    onQuickAdd = { onQuickAddInQuadrant(TaskPriority.P4) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun QuadrantCard(
    priority: TaskPriority,
    tasks: List<TaskEntity>,
    onToggleComplete: (TaskEntity) -> Unit,
    onMovePriority: (TaskEntity, TaskPriority) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(priority.colorHex)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .heightIn(min = 250.dp)
            .border(1.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Quadrant Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "${priority.shortTag} • ${priority.quadrantTitle}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = priority.quadrantSubtitle,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onQuickAdd,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add in ${priority.shortTag}",
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { task ->
                        QuadrantTaskRow(
                            task = task,
                            accent = accent,
                            onToggleComplete = { onToggleComplete(task) },
                            onMovePriority = { target -> onMovePriority(task, target) },
                            onEditTask = { onEditTask(task) },
                            onDeleteTask = { onDeleteTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantTaskRow(
    task: TaskEntity,
    accent: Color,
    onToggleComplete: () -> Unit,
    onMovePriority: (TaskPriority) -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditTask() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Complete",
                tint = accent,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onToggleComplete() }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${task.projectName}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Task",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onEditTask() }
            )

            Spacer(modifier = Modifier.width(5.dp))

            Box {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Move Quadrant",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { menuExpanded = true }
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    TaskPriority.entries.forEach { target ->
                        DropdownMenuItem(
                            text = { Text("Move to ${target.shortTag}: ${target.quadrantTitle}") },
                            onClick = {
                                menuExpanded = false
                                onMovePriority(target)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(5.dp))

            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Task",
                tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDeleteTask() }
            )
        }
    }
}
