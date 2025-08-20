package co.uk.doverguitarteacher.securenotesaug20th
import androidx.activity.result.PickVisualMediaRequest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts // <-- IMPORT THAT FIXES PickVisualMedia ERROR
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
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

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isEncrypted by remember { mutableStateOf(false) }
    var salt by remember { mutableStateOf<ByteArray?>(null) }
    var createdAt by remember { mutableStateOf(0L) }
    var imageFilename by remember { mutableStateOf<String?>(null) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var decryptedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf(PinAction.ENCRYPT) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageUri = uri
                decryptedBitmap = null
            }
        }
    )

    if (!isNewNote && noteId != null) {
        val note by viewModel.getNoteById(noteId).observeAsState()
        LaunchedEffect(note) {
            note?.let {
                title = it.title
                content = it.content
                isEncrypted = it.isEncrypted
                salt = it.salt
                createdAt = it.createdAt
                imageFilename = it.imageFilename
                decryptedBitmap = null
                imageUri = null
            }
        }
    }

    val onSaveNote: () -> Unit = {
        if (title.isBlank()) {
            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
        } else if (isEncrypted && salt == null) {
            pinAction = PinAction.ENCRYPT
            showPinDialog = true
        } else {
            coroutineScope.launch {
                val currentTime = System.currentTimeMillis()
                val finalCreatedAt = if (isNewNote) currentTime else createdAt
                var finalImageFilename = imageFilename

                imageUri?.let { uri ->
                    if (salt != null) {
                        val imageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        // Dummy PIN is fine here, as the content encryption is the master
                        val encryptedBytes = imageBytes?.let { EncryptionManager.encryptBytes(it, "000000", salt!!) }
                        val filename = "IMG_${UUID.randomUUID()}.enc"

                        if (encryptedBytes != null) {
                            withContext(Dispatchers.IO) {
                                imageFilename?.let { context.deleteFile(it) }
                                context.openFileOutput(filename, Context.MODE_PRIVATE).use { it.write(encryptedBytes) }
                            }
                            finalImageFilename = filename
                        }
                    } else {
                        val imageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        val filename = "IMG_${UUID.randomUUID()}.jpg"
                        if (imageBytes != null) {
                            withContext(Dispatchers.IO) {
                                imageFilename?.let { context.deleteFile(it) }
                                context.openFileOutput(filename, Context.MODE_PRIVATE).use { it.write(imageBytes) }
                            }
                            finalImageFilename = filename
                        }
                    }
                }

                val noteToSave = Note(
                    id = noteId ?: 0,
                    title = title,
                    content = content,
                    isEncrypted = isEncrypted,
                    salt = salt,
                    createdAt = finalCreatedAt,
                    updatedAt = currentTime,
                    imageFilename = finalImageFilename
                )
                if (isNewNote) viewModel.insert(noteToSave) else viewModel.update(noteToSave)
                navController.popBackStack()
            }
        }
    }

    val onDecryptNote: (String) -> Unit = { pin ->
        val biometricManager = BiometricManager(context)
        biometricManager.promptForAuthentication {
            coroutineScope.launch {
                val decryptedText = salt?.let { currentSalt -> EncryptionManager.decrypt(content, currentSalt, pin) }
                if (decryptedText != null) {
                    content = decryptedText
                    isEncrypted = false

                    imageFilename?.let { filename ->
                        withContext(Dispatchers.IO) {
                            try {
                                val encryptedImageBytes = context.openFileInput(filename).use { it.readBytes() }
                                val decryptedImageBytes = salt?.let { EncryptionManager.decryptBytes(encryptedImageBytes, pin, it) }
                                if (decryptedImageBytes != null) {
                                    decryptedBitmap = BitmapFactory.decodeByteArray(decryptedImageBytes, 0, decryptedImageBytes.size)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    salt = null
                    Toast.makeText(context, "Note Decrypted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
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
                    val payload = EncryptionManager.encrypt(content, pin)
                    content = payload.encryptedData
                    salt = payload.salt
                    isEncrypted = true
                    Toast.makeText(context, "Note is now encrypted. Tap Save.", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = onSaveNote) {
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
                        contentDescription = "Decrypted Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentScale = ContentScale.Crop
                    )
                } else if (imageUri != null) {
                    Image(
                        bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri!!)).asImageBitmap(),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentScale = ContentScale.Crop
                    )
                } else if (imageFilename != null && !isEncrypted) {
                    LaunchedEffect(imageFilename) {
                        withContext(Dispatchers.IO) {
                            try {
                                val bytes = context.openFileInput(imageFilename).use { it.readBytes() }
                                decryptedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            if (!isEncrypted) {
                Button(
                    onClick = {
                        // THIS IS THE CORRECTED LINE
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(if (imageFilename == null && imageUri == null) "Add Image" else "Change Image")
                }
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
