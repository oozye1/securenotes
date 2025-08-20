package co.uk.doverguitarteacher.securenotesaug20th

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

    // State for the text fields
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    // If it's an existing note, observe it from the database
    if (!isNewNote) {
        val note by viewModel.getNoteById(noteId!!).observeAsState()
        // Update the text fields once the note is loaded
        LaunchedEffect(note) {
            note?.let {
                title = it.title
                content = it.content
            }
        }
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

                            if (isNewNote) {
                                viewModel.insert(noteToSave)
                            } else {
                                viewModel.update(noteToSave)
                            }
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note")
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
                    .weight(1f) // Takes up remaining space
            )
        }
    }
}
