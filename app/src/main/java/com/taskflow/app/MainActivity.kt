package com.taskflow.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.app.data.model.TaskPriority
import com.taskflow.app.ui.MainViewModel
import com.taskflow.app.ui.components.QuickAddBottomSheet
import com.taskflow.app.ui.screens.CalendarTimelineScreen
import com.taskflow.app.ui.screens.EisenhowerMatrixScreen
import com.taskflow.app.ui.screens.HabitsAndStatsScreen
import com.taskflow.app.ui.screens.PomodoroFocusScreen
import com.taskflow.app.ui.screens.TasksScreen
import com.taskflow.app.ui.theme.TaskFlowTheme
import com.taskflow.app.widget.TaskFlowWidgetProvider
import kotlinx.coroutines.delay
import java.util.Locale

private enum class BottomNavTab(val label: String, val icon: ImageVector) {
    TASKS("Tasks", Icons.Default.CheckCircle),
    TIMELINE("Timeline", Icons.Default.CalendarMonth),
    MATRIX("Matrix", Icons.Default.GridView),
    POMODORO("Focus", Icons.Default.Timer),
    HABITS("Habits", Icons.Default.LocalFireDepartment)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialAction = intent?.action

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val projects by viewModel.projects.collectAsState()
            val filteredTasks by viewModel.filteredTasks.collectAsState()
            val allTasks by viewModel.allTasks.collectAsState()
            val subtasks by viewModel.allSubtasks.collectAsState()
            val habits by viewModel.habits.collectAsState()
            val habitLogs by viewModel.habitLogs.collectAsState()
            val focusSessions by viewModel.focusSessions.collectAsState()

            val selectedFilter by viewModel.selectedFilter.collectAsState()
            val selectedProjectId by viewModel.selectedProjectId.collectAsState()
            val selectedTag by viewModel.selectedTag.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val selectedCalendarDay by viewModel.selectedCalendarEpochDay.collectAsState()
            val pomodoroState by viewModel.pomodoroState.collectAsState()
            val bannerMessage by viewModel.statusBannerMessage.collectAsState()

            var currentTab by remember { mutableIntStateOf(0) }
            var showQuickAddSheet by remember {
                mutableStateOf(
                    initialAction == TaskFlowWidgetProvider.ACTION_QUICK_ADD ||
                        initialAction == TaskFlowWidgetProvider.ACTION_VOICE_ADD
                )
            }
            var quickAddInitialText by remember { mutableStateOf("") }
            var quickAddInitialPriority by remember { mutableStateOf<TaskPriority?>(null) }
            var showNewProjectDialog by remember { mutableStateOf(false) }

            // Pending Voice callback holder
            var pendingVoiceCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

            val speechRecognizerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val firstMatch = spokenMatches?.firstOrNull()
                    if (!firstMatch.isNullOrBlank()) {
                        pendingVoiceCallback?.invoke(firstMatch)
                    }
                }
            }

            fun launchVoiceRecognition(onSpoken: (String) -> Unit) {
                pendingVoiceCallback = onSpoken
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(
                        RecognizerIntent.EXTRA_PROMPT,
                        "Speak task (e.g., 'Call dentist tomorrow 3pm #Personal !p1')"
                    )
                }
                runCatching {
                    speechRecognizerLauncher.launch(speechIntent)
                }.onFailure {
                    viewModel.postBannerMessage("Voice recognizer unavailable on this device")
                }
            }

            // Auto-dismiss status banner after 3.5 seconds
            LaunchedEffect(bannerMessage) {
                if (bannerMessage != null) {
                    delay(3500L)
                    viewModel.clearBannerMessage()
                }
            }

            // If launched from Home Widget Voice button, trigger voice recognition automatically
            LaunchedEffect(initialAction) {
                if (initialAction == TaskFlowWidgetProvider.ACTION_VOICE_ADD) {
                    launchVoiceRecognition { spoken ->
                        quickAddInitialText = spoken
                        showQuickAddSheet = true
                    }
                }
            }

            TaskFlowTheme(darkTheme = isDarkMode) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "TaskFlow",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = "Todoist • TickTick • Google Tasks",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showNewProjectDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = "New Project List"
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleDarkMode() }) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Theme"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    floatingActionButton = {
                        if (currentTab != BottomNavTab.POMODORO.ordinal) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    quickAddInitialText = ""
                                    quickAddInitialPriority = null
                                    showQuickAddSheet = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Quick Add")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Add", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            BottomNavTab.entries.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = currentTab == index,
                                    onClick = { currentTab = index },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (BottomNavTab.entries[currentTab]) {
                            BottomNavTab.TASKS -> {
                                TasksScreen(
                                    tasks = filteredTasks,
                                    allTasks = allTasks,
                                    subtasks = subtasks,
                                    projects = projects,
                                    selectedFilter = selectedFilter,
                                    selectedProjectId = selectedProjectId,
                                    selectedTag = selectedTag,
                                    searchQuery = searchQuery,
                                    onSelectFilter = viewModel::setFilterMode,
                                    onSelectProject = viewModel::selectProjectFilter,
                                    onSelectTag = viewModel::selectTagFilter,
                                    onSearchChange = viewModel::setSearchQuery,
                                    onToggleComplete = viewModel::toggleTaskCompletion,
                                    onToggleStar = viewModel::toggleTaskStar,
                                    onDeleteTask = viewModel::deleteTask,
                                    onChangePriority = viewModel::changeTaskPriority,
                                    onRescheduleTask = viewModel::rescheduleTask,
                                    onAddSubtask = viewModel::addSubtask,
                                    onToggleSubtask = viewModel::toggleSubtask,
                                    onDeleteSubtask = viewModel::deleteSubtask,
                                    onStartFocusForTask = { task ->
                                        viewModel.selectTaskForPomodoro(task)
                                        currentTab = BottomNavTab.POMODORO.ordinal
                                    },
                                    onOpenVoiceQuickAdd = {
                                        quickAddInitialText = ""
                                        quickAddInitialPriority = null
                                        showQuickAddSheet = true
                                        launchVoiceRecognition { spoken ->
                                            quickAddInitialText = spoken
                                        }
                                    },
                                    onClearCompleted = viewModel::clearCompletedTasks
                                )
                            }

                            BottomNavTab.TIMELINE -> {
                                CalendarTimelineScreen(
                                    allTasks = allTasks,
                                    selectedEpochDay = selectedCalendarDay,
                                    onSelectEpochDay = viewModel::selectCalendarDay,
                                    onToggleComplete = viewModel::toggleTaskCompletion,
                                    onOpenQuickAddForSelectedDay = {
                                        quickAddInitialText = ""
                                        quickAddInitialPriority = null
                                        showQuickAddSheet = true
                                    }
                                )
                            }

                            BottomNavTab.MATRIX -> {
                                EisenhowerMatrixScreen(
                                    allTasks = allTasks,
                                    onToggleComplete = viewModel::toggleTaskCompletion,
                                    onMovePriority = viewModel::changeTaskPriority,
                                    onQuickAddInQuadrant = { prio ->
                                        quickAddInitialText = ""
                                        quickAddInitialPriority = prio
                                        showQuickAddSheet = true
                                    }
                                )
                            }

                            BottomNavTab.POMODORO -> {
                                PomodoroFocusScreen(
                                    timerState = pomodoroState,
                                    activeTasks = allTasks.filter { !it.isCompleted },
                                    focusSessions = focusSessions,
                                    onSelectPhase = viewModel::setPomodoroPhase,
                                    onSelectTask = viewModel::selectTaskForPomodoro,
                                    onToggleStartPause = viewModel::togglePomodoroStartPause,
                                    onReset = viewModel::resetPomodoro,
                                    onCycleAmbientSound = viewModel::cycleAmbientSound,
                                    onCompleteSessionNow = viewModel::completePomodoroNowForTesting
                                )
                            }

                            BottomNavTab.HABITS -> {
                                HabitsAndStatsScreen(
                                    habits = habits,
                                    habitLogs = habitLogs,
                                    allTasks = allTasks,
                                    focusSessions = focusSessions,
                                    onToggleHabitDay = viewModel::toggleHabitDay,
                                    onAddHabit = viewModel::addHabit,
                                    onDeleteHabit = viewModel::deleteHabit,
                                    onExportBackupJson = viewModel::exportBackupJson,
                                    onImportBackupJson = viewModel::importBackupJson,
                                    onShowMessage = viewModel::postBannerMessage
                                )
                            }
                        }

                        // Toast / Banner Snackbar
                        bannerMessage?.let { msg ->
                            Snackbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                action = {
                                    TextButton(onClick = { viewModel.clearBannerMessage() }) {
                                        Text("OK")
                                    }
                                }
                            ) {
                                Text(msg)
                            }
                        }
                    }
                }

                if (showQuickAddSheet) {
                    QuickAddBottomSheet(
                        projects = projects,
                        initialText = quickAddInitialText,
                        initialPriority = quickAddInitialPriority,
                        onRequestVoiceInput = { callback ->
                            launchVoiceRecognition(callback)
                        },
                        onDismiss = { showQuickAddSheet = false },
                        onSubmit = { rawInput, notes, overridePrio, overrideProj, overrideDay, overrideTime, overrideRec, overrideStar, subtasksList ->
                            viewModel.addTaskFromSmartInput(
                                rawInput = rawInput,
                                notes = notes,
                                overridePriority = overridePrio,
                                overrideProjectId = overrideProj,
                                overrideEpochDay = overrideDay,
                                overrideTimeMinutes = overrideTime,
                                overrideRecurrence = overrideRec,
                                overrideStarred = overrideStar,
                                subtaskLines = subtasksList
                            )
                        }
                    )
                }

                if (showNewProjectDialog) {
                    var newProjName by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showNewProjectDialog = false },
                        title = { Text("New Project / List") },
                        text = {
                            OutlinedTextField(
                                value = newProjName,
                                onValueChange = { newProjName = it },
                                label = { Text("Project Name (e.g., SideHustle)") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newProjName.isNotBlank()) {
                                        viewModel.addProject(newProjName, 0xFF8B5CF6)
                                        showNewProjectDialog = false
                                    }
                                }
                            ) {
                                Text("Create")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNewProjectDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
