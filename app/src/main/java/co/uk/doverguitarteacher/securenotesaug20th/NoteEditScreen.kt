package co.uk.doverguitarteacher.securenotesaug20th

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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

    // Add a state to track decrypted image bytes
    var decryptedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

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

    LaunchedEffect(imageUri, existingNote, isEncrypted) {
        decryptedBitmap = null
        if (isEncrypted && existingNote?.imageFilename != null && decryptedImageBytes != null) {
            decryptedBitmap = BitmapFactory.decodeByteArray(decryptedImageBytes, 0, decryptedImageBytes!!.size)
        } else when {
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

            // Only generate a new filename if a new image is picked
            var finalImageFilename: String? = existingNote?.imageFilename
            val imageToEncryptUri = imageUri ?: existingNote?.imageFilename?.let { Uri.fromFile(context.getFileStreamPath(it)) }

            val note = existingNote // assign to local variable for smart cast

            if (imageUri != null) {
                android.util.Log.d("NoteEditScreen", "Encrypting new image: $imageUri")
                finalImageFilename = withContext(Dispatchers.IO) {
                    val imageBytes = context.contentResolver.openInputStream(imageUri!!)?.use { it.readBytes() }
                    android.util.Log.d("NoteEditScreen", "Read imageBytes: ${imageBytes?.size}")
                    if (imageBytes != null) {
                        // Log first 16 bytes as hex before encryption
                        val hexPreview = imageBytes.take(16).joinToString(" ") { String.format("%02X", it) }
                        android.util.Log.d("NoteEditScreen", "Original image bytes preview: $hexPreview")
                        val encryptedBytes = EncryptionManager.encryptBytes(imageBytes, pin, newSalt)
                        android.util.Log.d("NoteEditScreen", "Encrypted imageBytes: ${encryptedBytes?.size}")
                        val newFilename = "IMG_${UUID.randomUUID()}.enc"
                        // Only delete old image file if a new image is picked
                        note?.imageFilename?.let {
                            android.util.Log.d("NoteEditScreen", "Deleting old image file: $it")
                            context.deleteFile(it)
                        }
                        if (encryptedBytes != null) {
                            context.openFileOutput(newFilename, Context.MODE_PRIVATE).use { it.write(encryptedBytes) }
                            android.util.Log.d("NoteEditScreen", "Saved encrypted image as: $newFilename")
                            // Log the first 16 bytes of encryptedBytes for debugging
                            val encryptedHexPreview = encryptedBytes.take(16).joinToString(" ") { String.format("%02X", it) }
                            android.util.Log.d("NoteEditScreen", "Encrypted image bytes preview: $encryptedHexPreview")
                            newFilename
                        } else null
                    } else null
                }
            } else if (note != null && note.imageFilename != null) {
                android.util.Log.d("NoteEditScreen", "No new image picked, keeping existing encrypted file: ${note.imageFilename}")
                // Do NOT re-encrypt the image file if no new image is picked
                finalImageFilename = note.imageFilename
            }

            val noteToSave = Note(
                id = noteId ?: 0, title = title, content = encryptedContent, isEncrypted = true, salt = newSalt,
                createdAt = existingNote?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(), imageFilename = finalImageFilename
            )
            if (isNewNote) {
                viewModel.insert(noteToSave)
            } else {
                viewModel.update(noteToSave)
            }
            // Reload note data to update existingNote and imageFilename
            viewModel.loadNoteForEdit(noteId)
            navController.popBackStack()
        }
    }

    val onDecryptNote = { pin: String ->
        val currentNote = viewModel.editNoteExistingData.value
        if (currentNote?.salt != null) {
            val currentSalt = currentNote.salt
            android.util.Log.d("NoteEditScreen", "Decrypting with PIN: $pin, salt: $currentSalt")
            val biometricManager = BiometricManager(context)
            biometricManager.promptForAuthentication {
                coroutineScope.launch {
                    android.util.Log.d("NoteEditScreen", "Starting decryption for noteId: ${currentNote.id}")
                    val decryptedText = EncryptionManager.decrypt(currentNote.content, currentSalt, pin)
                    android.util.Log.d("NoteEditScreen", "Decrypted text: ${decryptedText?.take(50)}")
                    if (decryptedText != null) {
                        viewModel.onContentChange(decryptedText)
                        isEncrypted = false
                        val imageFilenameToDecrypt = currentNote.imageFilename
                        android.util.Log.d("NoteEditScreen", "Attempting to decrypt image file: $imageFilenameToDecrypt")
                        imageFilenameToDecrypt?.let { filename ->
                            withContext(Dispatchers.IO) {
                                try {
                                    val encBytes = context.openFileInput(filename).use { it.readBytes() }
                                    android.util.Log.d("NoteEditScreen", "Read encrypted bytes: ${encBytes.size}")
                                    val decBytes = EncryptionManager.decryptBytes(encBytes, pin, currentSalt)
                                    android.util.Log.d("NoteEditScreen", "Decrypted image bytes: ${decBytes?.size}")
                                    if (decBytes != null) {
                                        // Log first 16 bytes as hex
                                        val hexPreview = decBytes.take(16).joinToString(" ") { String.format("%02X", it) }
                                        android.util.Log.d("NoteEditScreen", "Decrypted image bytes preview: $hexPreview")
                                        decryptedImageBytes = decBytes // Store decrypted bytes in state
                                        try {
                                            decryptedBitmap = BitmapFactory.decodeByteArray(decBytes, 0, decBytes.size)
                                            android.util.Log.d("NoteEditScreen", "Set decryptedBitmap: ${decryptedBitmap != null}")
                                            if (decryptedBitmap != null) {
                                                android.util.Log.d("NoteEditScreen", "Bitmap dimensions: ${decryptedBitmap!!.width}x${decryptedBitmap!!.height}")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("NoteEditScreen", "BitmapFactory.decodeByteArray exception", e)
                                        }
                                    } else {
                                        android.util.Log.e("NoteEditScreen", "Decryption failed: decBytes is null")
                                        withContext(Dispatchers.Main) { decryptedBitmap = null }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("NoteEditScreen", "Exception during decryption", e)
                                    withContext(Dispatchers.Main) { decryptedBitmap = null }
                                }
                            }
                        }
                        Toast.makeText(context, "Note Decrypted!", Toast.LENGTH_SHORT).show()
                    } else {
                        android.util.Log.e("NoteEditScreen", "Decryption failed: decryptedText is null")
                        Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            android.util.Log.e("NoteEditScreen", "Decryption failed: salt is null for noteId: ${currentNote?.id}")
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
