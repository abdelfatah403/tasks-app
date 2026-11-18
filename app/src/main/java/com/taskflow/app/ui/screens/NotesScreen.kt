package com.taskflow.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.taskflow.app.data.model.NoteEntity
import com.taskflow.app.data.model.ProjectEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val noteColorPalette = listOf(
    0xFF6366F1L to "Indigo",
    0xFF10B981L to "Emerald",
    0xFFF59E0BL to "Amber",
    0xFFEC4899L to "Rose",
    0xFF8B5CF6L to "Violet",
    0xFF06B6D4L to "Cyan",
    0xFF64748BL to "Slate"
)

@Composable
fun NotesScreen(
    notes: List<NoteEntity>,
    projects: List<ProjectEntity>,
    onAddNote: (title: String, content: String, projectId: Long, tagsCsv: String, colorHex: Long, isPinned: Boolean) -> Unit,
    onUpdateNote: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onConvertToTask: (NoteEntity) -> Unit,
    onRequestVoiceInput: ((String) -> Unit) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var showCreateNoteSheet by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val filteredNotes = remember(notes, searchQuery, selectedProjectId) {
        notes.filter { note ->
            val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true) ||
                note.tagsCsv.contains(searchQuery, ignoreCase = true)
            val matchesProject = selectedProjectId == null || note.projectId == selectedProjectId
            matchesSearch && matchesProject
        }
    }

    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val unpinnedNotes = filteredNotes.filter { !it.isPinned }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Banner + New Note Button
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Notes & Scratchpad (${notes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Color-coded notes, checklists & 1-tap Convert to Task",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showCreateNoteSheet = true },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes, ideas, or @tags...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. Project Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedProjectId == null,
                    onClick = { selectedProjectId = null },
                    label = { Text("All Notes") }
                )
                projects.forEach { proj ->
                    FilterChip(
                        selected = selectedProjectId == proj.id,
                        onClick = {
                            selectedProjectId = if (selectedProjectId == proj.id) null else proj.id
                        },
                        label = { Text("#${proj.name}") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(proj.colorHex))
                            )
                        }
                    )
                }
            }
        }

        // 4. Empty State
        if (filteredNotes.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
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
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No notes found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap '+ New Note' above to jot down ideas, meeting notes, or checklists.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Pinned Notes Section
        if (pinnedNotes.isNotEmpty()) {
            item {
                Text(
                    text = "📌 Pinned Notes (${pinnedNotes.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                NoteCardItem(
                    note = note,
                    onEdit = { editingNote = note },
                    onTogglePin = { onTogglePin(note) },
                    onConvertToTask = { onConvertToTask(note) },
                    onDelete = { onDeleteNote(note) }
                )
            }
        }

        // 6. Other Notes Section
        if (unpinnedNotes.isNotEmpty()) {
            if (pinnedNotes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All Notes (${unpinnedNotes.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(unpinnedNotes, key = { "note_${it.id}" }) { note ->
                NoteCardItem(
                    note = note,
                    onEdit = { editingNote = note },
                    onTogglePin = { onTogglePin(note) },
                    onConvertToTask = { onConvertToTask(note) },
                    onDelete = { onDeleteNote(note) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    // Create New Note Bottom Sheet
    if (showCreateNoteSheet) {
        NoteEditorBottomSheet(
            initialNote = null,
            projects = projects,
            onRequestVoiceInput = onRequestVoiceInput,
            onDismiss = { showCreateNoteSheet = false },
            onSave = { title, content, projectId, tagsCsv, colorHex, isPinned ->
                onAddNote(title, content, projectId, tagsCsv, colorHex, isPinned)
                showCreateNoteSheet = false
            }
        )
    }

    // Edit Existing Note Bottom Sheet
    editingNote?.let { targetNote ->
        NoteEditorBottomSheet(
            initialNote = targetNote,
            projects = projects,
            onRequestVoiceInput = onRequestVoiceInput,
            onDismiss = { editingNote = null },
            onSave = { title, content, projectId, tagsCsv, colorHex, isPinned ->
                val projName = projects.find { it.id == projectId }?.name ?: targetNote.projectName
                val cleanedTags = tagsCsv
                    .split(",")
                    .map { it.trim().removePrefix("@") }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")
                onUpdateNote(
                    targetNote.copy(
                        title = title.trim().ifBlank { targetNote.title },
                        content = content.trim(),
                        projectId = projectId,
                        projectName = projName,
                        tagsCsv = cleanedTags,
                        colorHex = colorHex,
                        isPinned = isPinned,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                editingNote = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCardItem(
    note: NoteEntity,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onConvertToTask: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = Color(note.colorHex)
    val dateFormatted = remember(note.updatedAtMillis) {
        SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(note.updatedAtMillis))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.10f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Direct 1-Tap Actions: Pin + Convert to Task + Edit + Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onConvertToTask, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = "Convert Note to Task",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "#${note.projectName}",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                note.tagsList.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "@$tag",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Updated $dateFormatted",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp, start = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorBottomSheet(
    initialNote: NoteEntity?,
    projects: List<ProjectEntity>,
    onRequestVoiceInput: ((String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, projectId: Long, tagsCsv: String, colorHex: Long, isPinned: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(initialNote?.id) { mutableStateOf(initialNote?.title ?: "") }
    var content by remember(initialNote?.id) { mutableStateOf(initialNote?.content ?: "") }
    var selectedProjectId by remember(initialNote?.id) {
        mutableLongStateOf(initialNote?.projectId ?: (projects.firstOrNull()?.id ?: 1L))
    }
    var tagsCsv by remember(initialNote?.id) { mutableStateOf(initialNote?.tagsCsv ?: "") }
    var selectedColorHex by remember(initialNote?.id) {
        mutableLongStateOf(initialNote?.colorHex ?: 0xFF6366F1L)
    }
    var isPinned by remember(initialNote?.id) { mutableStateOf(initialNote?.isPinned ?: false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialNote == null) "Create New Note" else "Edit Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable {
                            onRequestVoiceInput { spoken ->
                                if (title.isBlank()) {
                                    title = spoken
                                } else {
                                    content = if (content.isBlank()) spoken else "$content\n$spoken"
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate Note",
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

                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) Color(selectedColorHex) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note Title") },
                placeholder = { Text("e.g., Sprint Planning Ideas") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Note Content / Checklist / Details") },
                placeholder = { Text("Write your thoughts, bullet points, or meeting notes...") },
                minLines = 5,
                maxLines = 10,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Color Picker Row
            Text(
                text = "Card Color Accent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                noteColorPalette.forEach { (hex, _) ->
                    val isSelected = selectedColorHex == hex
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Project Selector
            Text(
                text = "Project / Folder",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projects.forEach { proj ->
                    FilterChip(
                        selected = selectedProjectId == proj.id,
                        onClick = { selectedProjectId = proj.id },
                        label = { Text(proj.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = tagsCsv,
                onValueChange = { tagsCsv = it },
                label = { Text("Tags (comma-separated)") },
                placeholder = { Text("ideas, meeting, summary") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

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
                        onSave(
                            title,
                            content,
                            selectedProjectId,
                            tagsCsv,
                            selectedColorHex,
                            isPinned
                        )
                    },
                    enabled = title.isNotBlank() || content.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (initialNote == null) "Save Note" else "Update Note",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
