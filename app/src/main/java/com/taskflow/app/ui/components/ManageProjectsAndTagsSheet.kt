package com.taskflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.taskflow.app.data.model.TagEntity

private val presetColors = listOf(
    0xFF6366F1L, // Indigo
    0xFFEC4899L, // Pink
    0xFF10B981L, // Emerald
    0xFFF59E0BL, // Amber
    0xFF06B6D4L, // Cyan
    0xFF8B5CF6L, // Violet
    0xFFEF4444L  // Red
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProjectsAndTagsSheet(
    projects: List<ProjectEntity>,
    tags: List<TagEntity>,
    allUsedTagNames: List<String>,
    onAddProject: (String, Long) -> Unit,
    onUpdateProject: (ProjectEntity, String, Long) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit,
    onAddTag: (String, Long) -> Unit,
    onRenameTag: (oldName: String, newName: String, colorHex: Long) -> Unit,
    onDeleteTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Projects, 1 = Tags

    // New Project / Edit Project state
    var newProjectName by remember { mutableStateOf("") }
    var newProjectColor by remember { mutableLongStateOf(presetColors.first()) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var editProjectName by remember { mutableStateOf("") }
    var editProjectColor by remember { mutableLongStateOf(presetColors.first()) }

    // New Tag / Edit Tag state
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableLongStateOf(0xFF8B5CF6L) }
    var editingTagOldName by remember { mutableStateOf<String?>(null) }
    var editTagNewName by remember { mutableStateOf("") }
    var editTagColor by remember { mutableLongStateOf(0xFF8B5CF6L) }

    // Merge DB tags + any ad-hoc tags on tasks/notes so every single tag can be edited or deleted
    val combinedTags = remember(tags, allUsedTagNames) {
        val dbMap = tags.associateBy { it.name.lowercase() }
        val allNames = (tags.map { it.name.lowercase() } + allUsedTagNames.map { it.lowercase() }).distinct()
        allNames.map { name ->
            dbMap[name] ?: TagEntity(name = name, colorHex = 0xFF8B5CF6L)
        }
    }

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
            Text(
                text = "Manage #Projects & @Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add, rename, recolor, or delete any project list or @tag",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Section Switcher (Projects vs Tags)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text("#Projects (${projects.size})", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text("@Tags (${combinedTags.size})", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedSection == 0) {
                // ================= PROJECTS SECTION =================
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (editingProject == null) "Create New #Project" else "Update #${editingProject?.name}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = if (editingProject == null) newProjectName else editProjectName,
                            onValueChange = {
                                if (editingProject == null) newProjectName = it else editProjectName = it
                            },
                            placeholder = { Text("Project name (e.g., Study, SideHustle)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Color Swatches
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeColor = if (editingProject == null) newProjectColor else editProjectColor
                            presetColors.forEach { hex ->
                                val isSel = activeColor == hex
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (isSel) 2.dp else 0.dp,
                                            color = if (isSel) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (editingProject == null) newProjectColor = hex else editProjectColor = hex
                                        }
                                ) {
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (editingProject != null) {
                                OutlinedButton(
                                    onClick = { editingProject = null },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }
                            Button(
                                onClick = {
                                    if (editingProject == null) {
                                        if (newProjectName.isNotBlank()) {
                                            onAddProject(newProjectName.trim(), newProjectColor)
                                            newProjectName = ""
                                        }
                                    } else {
                                        val target = editingProject!!
                                        if (editProjectName.isNotBlank()) {
                                            onUpdateProject(target, editProjectName.trim(), editProjectColor)
                                            editingProject = null
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(
                                    imageVector = if (editingProject == null) Icons.Default.Add else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editingProject == null) "Add Project" else "Save Project")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Existing Projects (Tap ✏️ to update or 🗑️ to delete)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    projects.forEach { proj ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(proj.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "#${proj.name}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        if (proj.id == 1L && proj.isDefault) {
                                            Text(
                                                text = "Default Inbox (can rename/recolor)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingProject = proj
                                            editProjectName = proj.name
                                            editProjectColor = proj.colorHex
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Project",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (!(proj.id == 1L && proj.isDefault)) {
                                        IconButton(
                                            onClick = { onDeleteProject(proj) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Project",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ================= TAGS SECTION =================
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (editingTagOldName == null) "Create New @Tag" else "Rename @$editingTagOldName",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = if (editingTagOldName == null) newTagName else editTagNewName,
                            onValueChange = {
                                if (editingTagOldName == null) newTagName = it else editTagNewName = it
                            },
                            placeholder = { Text("Tag name without @ (e.g., urgent, call)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (editingTagOldName != null) {
                                OutlinedButton(
                                    onClick = { editingTagOldName = null },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }

                            Button(
                                onClick = {
                                    if (editingTagOldName == null) {
                                        if (newTagName.isNotBlank()) {
                                            onAddTag(newTagName.trim(), newTagColor)
                                            newTagName = ""
                                        }
                                    } else {
                                        val old = editingTagOldName!!
                                        if (editTagNewName.isNotBlank()) {
                                            onRenameTag(old, editTagNewName.trim(), editTagColor)
                                            editingTagOldName = null
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(
                                    imageVector = if (editingTagOldName == null) Icons.Default.Add else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editingTagOldName == null) "Add @Tag" else "Save @Tag")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Existing @Tags (Tap ✏️ to rename everywhere or 🗑️ to delete)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    combinedTags.forEach { tag ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "@${tag.name}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingTagOldName = tag.name
                                            editTagNewName = tag.name
                                            editTagColor = tag.colorHex
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename Tag",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteTag(tag.name) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Tag",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Done")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
