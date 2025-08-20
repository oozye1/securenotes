package co.uk.doverguitarteacher.securenotesaug20th

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.uk.doverguitarteacher.securenotesaug20th.ui.theme.SecureNotesTheme

class MainActivity : FragmentActivity() {

    private lateinit var biometricManager: BiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricManager = BiometricManager(this)

        setContent {
            SecureNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(false) }

                    // --- THIS IS THE CRITICAL FIX ---
                    // This new auto-lock logic uses a timestamp to avoid locking instantly.
                    var lastStopTime by remember { mutableStateOf(0L) }
                    val lifecycleOwner = LocalLifecycleOwner.current

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_STOP -> {
                                    // When the app goes to the background, just record the time.
                                    lastStopTime = System.currentTimeMillis()
                                }
                                Lifecycle.Event.ON_START -> {
                                    // When it comes back, check if enough time has passed.
                                    val thirtySecondsInMillis = 30 * 1000
                                    if (System.currentTimeMillis() - lastStopTime > thirtySecondsInMillis) {
                                        // Only re-lock if it's been more than 30 seconds.
                                        isAuthenticated = false
                                    }
                                }
                                else -> { /* Do nothing */ }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    if (isAuthenticated) {
                        NotesAppNavigation()
                    } else {
                        // This will now only run on first launch, or after a long pause.
                        LaunchedEffect(isAuthenticated) {
                            if (!isAuthenticated) {
                                biometricManager.promptForAuthentication {
                                    isAuthenticated = true
                                }
                            }
                        }
                        LockedScreen(
                            onUnlockClick = {
                                biometricManager.promptForAuthentication {
                                    isAuthenticated = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotesAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val owner = LocalSavedStateRegistryOwner.current

    val noteViewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(
            (context.applicationContext as NotesApplication).repository,
            owner
        )
    )

    NavHost(navController = navController, startDestination = "note_list") {
        composable("note_list") {
            NoteListScreen(
                viewModel = noteViewModel,
                onAddNote = { navController.navigate("note_edit") },
                onNoteClick = { note -> navController.navigate("note_edit?noteId=${note.id}") }
            )
        }
        composable(
            route = "note_edit?noteId={noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            SecureScreen {
                val noteId = backStackEntry.arguments?.getInt("noteId")
                NoteEditScreen(
                    navController = navController,
                    viewModel = noteViewModel,
                    noteId = if (noteId == -1) null else noteId
                )
            }
        }
    }
}
