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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.model.FocusSessionEntity
import com.taskflow.app.data.model.HabitEntity
import com.taskflow.app.data.model.HabitLogEntity
import com.taskflow.app.data.model.TaskEntity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitsAndStatsScreen(
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>,
    allTasks: List<TaskEntity>,
    focusSessions: List<FocusSessionEntity>,
    onToggleHabitDay: (Long, Long) -> Unit,
    onAddHabit: (String, String, Long, Int) -> Unit,
    onDeleteHabit: (HabitEntity) -> Unit,
    onExportBackupJson: suspend () -> String,
    onReadLatestLocalBackup: suspend () -> String? = { null },
    onImportBackupJson: (String) -> Unit,
    onSaveBackupToFile: () -> Unit = {},
    onLoadBackupFromFile: () -> Unit = {},
    onShowMessage: (String) -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val last7Days = remember(todayEpoch) {
        (6L downTo 0L).map { offset -> LocalDate.ofEpochDay(todayEpoch - offset) }
    }

    var showNewHabitDialog by remember { mutableStateOf(false) }
    var showBackupModal by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // Karma & Productivity Metrics
    val completedTasks = allTasks.count { it.isCompleted }
    val totalFocusMins = focusSessions.sumOf { it.durationMinutes }
    val totalHabitCheckins = habitLogs.size
    val karmaScore = 100 + (completedTasks * 15) + (totalHabitCheckins * 10) + (totalFocusMins * 2)
    val karmaRank = when {
        karmaScore >= 1000 -> "Grandmaster 🏆"
        karmaScore >= 500 -> "Productivity Master ⚡"
        karmaScore >= 250 -> "Focused Pro 🔥"
        else -> "Apprentice 🌱"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Karma & Productivity Stats Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
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
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = karmaRank,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$karmaScore Karma Points • 100% Offline Vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatMetricBox(value = "$completedTasks", label = "Tasks Done")
                        StatMetricBox(value = "$totalHabitCheckins", label = "Habit Check-ins")
                        StatMetricBox(value = "${totalFocusMins}m", label = "Focused Time")
                    }
                }
            }
        }

        // 2. Daily Habit Tracker Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Habit Tracker (Last 7 Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any day circle to log or unlog your habit streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showNewHabitDialog = true },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Habit", fontSize = 12.sp)
                }
            }
        }

        // 3. Habit Cards with 7-Day Interactive Grid
        items(habits, key = { it.id }) { habit ->
            val logsForHabit = habitLogs.filter { it.habitId == habit.id }.map { it.epochDay }.toSet()
            val currentStreak = calculateStreak(logsForHabit, todayEpoch)
            val accentColor = Color(habit.colorHex)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(text = habit.emoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$currentStreak day streak • Goal: ${habit.targetDaysPerWeek}d/wk",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onDeleteHabit(habit) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Habit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 7-Day Bubbles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        last7Days.forEach { date ->
                            val dEpoch = date.toEpochDay()
                            val isDone = logsForHabit.contains(dEpoch)
                            val dayInitial = date.dayOfWeek
                                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .take(2)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onToggleHabitDay(habit.id, dEpoch) }
                            ) {
                                Text(
                                    text = dayInitial,
                                    fontSize = 10.sp,
                                    color = if (dEpoch == todayEpoch) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (dEpoch == todayEpoch) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDone) accentColor else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (dEpoch == todayEpoch) 2.dp else 0.dp,
                                            color = if (dEpoch == todayEpoch) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Done",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Local Offline JSON Backup & Restore Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "100% Local & Offline Data Vault",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "All tasks, notes, projects, tags, habits, and focus sessions are stored privately on-device. Export or restore a full JSON backup anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    backupJsonText = onExportBackupJson()
                                    clipboard.setText(AnnotatedString(backupJsonText))
                                    showBackupModal = true
                                    onShowMessage("✅ Backup exported, saved to local snapshot & copied to clipboard!")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val clipText = clipboard.getText()?.text?.trim().orEmpty()
                                    val localSnap = onReadLatestLocalBackup().orEmpty()
                                    backupJsonText = when {
                                        clipText.startsWith("{") -> clipText
                                        localSnap.isNotBlank() -> localSnap
                                        else -> ""
                                    }
                                    showBackupModal = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Direct File Picker Save & Load Row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onSaveBackupToFile,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save .json File", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onLoadBackupFromFile,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open .json File", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showNewHabitDialog) {
        NewHabitDialog(
            onDismiss = { showNewHabitDialog = false },
            onConfirm = { title, emoji, colorHex, days ->
                onAddHabit(title, emoji, colorHex, days)
                showNewHabitDialog = false
            }
        )
    }

    if (showBackupModal) {
        AlertDialog(
            onDismissRequest = { showBackupModal = false },
            title = { Text("Offline JSON Backup & Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Use the quick actions below to paste from clipboard, load your latest on-device snapshot, or edit/inspect the JSON before restoring:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboard.getText()?.text.orEmpty()
                                if (clip.isNotBlank()) {
                                    backupJsonText = clip
                                    onShowMessage("Pasted JSON from clipboard!")
                                } else {
                                    onShowMessage("Clipboard is empty")
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Clipboard", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val snap = onReadLatestLocalBackup()
                                    if (!snap.isNullOrBlank()) {
                                        backupJsonText = snap
                                        onShowMessage("Loaded latest local snapshot!")
                                    } else {
                                        onShowMessage("No local snapshot found — tap Export JSON first")
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Snapshot", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        placeholder = { Text("Backup JSON content...") },
                        minLines = 5,
                        maxLines = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val candidate = backupJsonText.ifBlank {
                            clipboard.getText()?.text.orEmpty()
                        }
                        if (candidate.isNotBlank()) {
                            onImportBackupJson(candidate)
                            showBackupModal = false
                        } else {
                            onShowMessage("Please paste or load a backup JSON first")
                        }
                    }
                ) {
                    Text("Restore Backup Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun StatMetricBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NewHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🔥") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Daily Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Name (e.g., 30m Coding)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Icon Emoji (e.g., 🔥 📚 💪 🧘)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, emoji, 0xFF10B981, 7)
                    }
                }
            ) {
                Text("Add Habit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun calculateStreak(completedDays: Set<Long>, todayEpoch: Long): Int {
    var streak = 0
    var checkDay = if (completedDays.contains(todayEpoch)) todayEpoch else todayEpoch - 1
    while (completedDays.contains(checkDay)) {
        streak++
        checkDay--
    }
    return streak
}
