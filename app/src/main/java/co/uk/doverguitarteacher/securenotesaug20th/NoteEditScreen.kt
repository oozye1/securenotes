package co.uk.doverguitarteacher.securenotesaug20th

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    navController: NavController,
    viewModel: NoteViewModel,
    noteId: Int?
) {
    val context = LocalContext.current as FragmentActivity
    val isNewNote = noteId == null

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isEncrypted by remember { mutableStateOf(false) }
    var salt by remember { mutableStateOf<ByteArray?>(null) }
    var createdAt by remember { mutableStateOf(0L) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf(PinAction.ENCRYPT) }

    if (!isNewNote && noteId != null) {
        val note by viewModel.getNoteById(noteId).observeAsState()
        LaunchedEffect(note) {
            note?.let {
                title = it.title
                content = it.content
                isEncrypted = it.isEncrypted
                salt = it.salt
                createdAt = it.createdAt
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            action = pinAction,
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                showPinDialog = false
                if (pinAction == PinAction.ENCRYPT) {
                    val payload = EncryptionManager.encrypt(content, pin)
                    content = payload.encryptedData
                    salt = payload.salt
                    isEncrypted = true
                    Toast.makeText(context, "Note Encrypted!", Toast.LENGTH_SHORT).show()
                } else { // DECRYPT
                    val biometricManager = BiometricManager(context)
                    biometricManager.promptForAuthentication {
                        val decryptedContent = salt?.let { currentSalt ->
                            EncryptionManager.decrypt(content, currentSalt, pin)
                        }
                        if (decryptedContent != null) {
                            content = decryptedContent
                            isEncrypted = false
                            salt = null
                            Toast.makeText(context, "Note Decrypted!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
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
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
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
                        pinAction = if (isEncrypted) PinAction.DECRYPT else PinAction.ENCRYPT
                        showPinDialog = true
                    }) {
                        Icon(
                            imageVector = if (isEncrypted) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = if (isEncrypted) "Decrypt Note" else "Encrypt Note"
                        )
                    }

                    if (!isNewNote) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Note")
                        }
                    }

                    IconButton(onClick = {
                        if (title.isNotBlank()) {
                            val currentTime = System.currentTimeMillis()
                            val finalCreatedAt = if (isNewNote) currentTime else createdAt

                            val noteToSave = Note(
                                id = noteId ?: 0,
                                title = title,
                                content = content,
                                isEncrypted = isEncrypted,
                                salt = salt,
                                createdAt = finalCreatedAt,
                                updatedAt = currentTime
                            )
                            if (isNewNote) viewModel.insert(noteToSave) else viewModel.update(noteToSave)
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = isEncrypted
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                readOnly = isEncrypted
            )
        }
    }
}

private enum class PinAction { ENCRYPT, DECRYPT }

@Composable
private fun PinDialog(
    action: PinAction,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val title = if (action == PinAction.ENCRYPT) "Set a 6-Digit PIN" else "Enter 6-Digit PIN"
    val buttonText = if (action == PinAction.ENCRYPT) "Encrypt" else "Decrypt"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
                label = { Text("PIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (pin.length == 6) onConfirm(pin) },
                enabled = pin.length == 6
            ) { Text(buttonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
