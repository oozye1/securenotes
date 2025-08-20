package co.uk.doverguitarteacher.securenotesaug20th

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.uk.doverguitarteacher.securenotesaug20th.ui.theme.SecureNotesTheme

class MainActivity : FragmentActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

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

                    // --- AUTO-LOCK LOGIC ---
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            // When the app is stopped (goes to background), reset authentication
                            if (event == Lifecycle.Event.ON_STOP) {
                                isAuthenticated = false
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    if (isAuthenticated) {
                        NotesAppNavigation(noteViewModel)
                    } else {
                        LaunchedEffect(Unit) {
                            biometricManager.promptForAuthentication {
                                isAuthenticated = true
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
fun NotesAppNavigation(noteViewModel: NoteViewModel) {
    val navController = rememberNavController()

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
            // --- SCREENSHOT PREVENTION LOGIC ---
            // Wrap the entire edit screen with our SecureScreen composable
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
