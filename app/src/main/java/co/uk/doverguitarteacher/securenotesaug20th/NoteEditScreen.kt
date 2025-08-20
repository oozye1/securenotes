package co.uk.doverguitarteacher.securenotesaug20th

import co.uk.doverguitarteacher.securenotesaug20th.BuildConfig
import androidx.activity.result.PickVisualMediaRequest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    navController: NavController,
    viewModel: NoteViewModel,
    noteId: Int?
) {
    val context = LocalContext.current as FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val isNewNote = noteId == null

    val title by viewModel.editNoteTitle.collectAsState()
    val content by viewModel.editNoteContent.collectAsState()
    val imageUri by viewModel.editNoteImageUri.collectAsState()
    val existingNote by viewModel.editNoteExistingData.collectAsState()
    val tempCameraUri by viewModel.tempCameraImageUri.collectAsState()

    var isEncrypted by remember(existingNote) { mutableStateOf(existingNote?.isEncrypted ?: false) }
    var decryptedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf(PinAction.ENCRYPT) }

    LaunchedEffect(Unit) {
        viewModel.loadNoteForEdit(noteId)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) viewModel.onImageUriChange(uri) }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) viewModel.onImageUriChange(tempCameraUri) }
    )

    LaunchedEffect(imageUri, existingNote) {
        decryptedBitmap = null
        when {
            imageUri != null -> {
                withContext(Dispatchers.IO) {
                    try {
                        decryptedBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri!!))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            existingNote != null && !existingNote!!.isEncrypted && existingNote!!.imageFilename != null -> {
                withContext(Dispatchers.IO) {
                    try {
                        val bytes = context.openFileInput(existingNote!!.imageFilename!!).use { it.readBytes() }
                        decryptedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    val onSaveUnencryptedNote = {
        coroutineScope.launch {
            if (title.isBlank()) {
                Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            var finalImageFilename = existingNote?.imageFilename
            if (imageUri != null) {
                finalImageFilename = withContext(Dispatchers.IO) {
                    val imageBytes = context.contentResolver.openInputStream(imageUri!!)?.use { it.readBytes() }
                    if (imageBytes != null) {
                        val newFilename = "IMG_${UUID.randomUUID()}.jpg"
                        existingNote?.imageFilename?.let { context.deleteFile(it) }
                        context.openFileOutput(newFilename, Context.MODE_PRIVATE).use { it.write(imageBytes) }
                        newFilename
                    } else { existingNote?.imageFilename }
                }
            }
            val noteToSave = Note(
                id = noteId ?: 0, title = title, content = content, isEncrypted = false, salt = null,
                createdAt = existingNote?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(), imageFilename = finalImageFilename
            )
            if (isNewNote) viewModel.insert(noteToSave) else viewModel.update(noteToSave)
            navController.popBackStack()
        }
    }

    val onEncryptAndSave = { pin: String ->
        coroutineScope.launch {
            if (title.isBlank()) {
                Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val payload = EncryptionManager.encrypt(content, pin)
            val encryptedContent = payload.encryptedData
            val newSalt = payload.salt

            var finalImageFilename = existingNote?.imageFilename
            val imageToEncryptUri = imageUri ?: existingNote?.imageFilename?.let { Uri.fromFile(context.getFileStreamPath(it)) }

            if (imageToEncryptUri != null) {
                finalImageFilename = withContext(Dispatchers.IO) {
                    val imageBytes = context.contentResolver.openInputStream(imageToEncryptUri)?.use { it.readBytes() }
                    if (imageBytes != null) {
                        val encryptedBytes = EncryptionManager.encryptBytes(imageBytes, pin, newSalt)
                        val newFilename = "IMG_${UUID.randomUUID()}.enc"
                        if (encryptedBytes != null) {
                            existingNote?.imageFilename?.let { context.deleteFile(it) }
                            context.openFileOutput(newFilename, Context.MODE_PRIVATE).use { it.write(encryptedBytes) }
                            newFilename
                        } else { existingNote?.imageFilename }
                    } else { existingNote?.imageFilename }
                }
            }

            val noteToSave = Note(
                id = noteId ?: 0, title = title, content = encryptedContent, isEncrypted = true, salt = newSalt,
                createdAt = existingNote?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(), imageFilename = finalImageFilename
            )
            if (isNewNote) viewModel.insert(noteToSave) else viewModel.update(noteToSave)
            navController.popBackStack()
        }
    }

    val onDecryptNote = { pin: String ->
        existingNote?.let { currentNote ->
            val currentSalt = currentNote.salt ?: return@let
            val biometricManager = BiometricManager(context)
            biometricManager.promptForAuthentication {
                coroutineScope.launch {
                    val decryptedText = EncryptionManager.decrypt(currentNote.content, currentSalt, pin)
                    if (decryptedText != null) {
                        viewModel.onContentChange(decryptedText)
                        isEncrypted = false
                        currentNote.imageFilename?.let { filename ->
                            withContext(Dispatchers.IO) {
                                try {
                                    val encBytes = context.openFileInput(filename).use { it.readBytes() }
                                    val decBytes = EncryptionManager.decryptBytes(encBytes, pin, currentSalt)
                                    if (decBytes != null) {
                                        decryptedBitmap = BitmapFactory.decodeByteArray(decBytes, 0, decBytes.size)
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                        Toast.makeText(context, "Note Decrypted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                    }
                }
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
                    onEncryptAndSave(pin)
                } else {
                    onDecryptNote(pin)
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
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
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
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete Note") }
                    }
                    // --- THIS IS THE CRITICAL FIX ---
                    // The `onClick` lambda now correctly calls the save function.
                    IconButton(onClick = { onSaveUnencryptedNote() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note")
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = isEncrypted
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.onContentChange(it) },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                readOnly = isEncrypted
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (decryptedBitmap != null) {
                    Image(
                        bitmap = decryptedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentScale = ContentScale.Crop
                    )
                } else if (isEncrypted && existingNote?.imageFilename != null) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Encrypted Image",
                        modifier = Modifier.size(128.dp)
                    )
                }
            }

            if (!isEncrypted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Add from Gallery")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Gallery")
                    }
                    Button(
                        onClick = {
                            val newUri = viewModel.createTempCameraUri(context)
                            cameraLauncher.launch(newUri)
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Take Photo")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Camera")
                    }
                }
                Text(
                    text = if (existingNote?.imageFilename == null && imageUri == null) "Add an Image" else "Change Image",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
    val title = if (action == PinAction.ENCRYPT) "Set a 6-Digit PIN to Encrypt" else "Enter 6-Digit PIN"
    val buttonText = if (action == PinAction.ENCRYPT) "Encrypt & Save" else "Decrypt"

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
