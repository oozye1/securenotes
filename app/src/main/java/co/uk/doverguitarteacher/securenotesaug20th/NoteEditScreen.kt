package co.uk.doverguitarteacher.securenotesaug20th

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    navController: NavController,
    viewModel: NoteViewModel,
    noteId: Int?
) {
    val context = LocalContext.current
    val isNewNote = noteId == null

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (!isNewNote) {
        val note by viewModel.getNoteById(noteId!!).observeAsState()
        LaunchedEffect(note) {
            note?.let {
                title = it.title
                content = it.content
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to permanently delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteById(noteId!!)
                        showDeleteDialog = false
                        Toast.makeText(context, "Note Deleted", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewNote) "Add Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (title.isNotBlank()) {
                            val noteToSave = if (isNewNote) {
                                Note(title = title, content = content)
                            } else {
                                Note(id = noteId!!, title = title, content = content)
                            }

                            if (isNewNote) viewModel.insert(noteToSave) else viewModel.update(noteToSave)
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note")
                    }

                    if (!isNewNote) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Note")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                // THIS IS THE LINE I FIXED. onValue-Change -> onValueChange
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
