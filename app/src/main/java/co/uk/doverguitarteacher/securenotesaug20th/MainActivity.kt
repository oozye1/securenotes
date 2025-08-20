package co.uk.doverguitarteacher.securenotesaug20th

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
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

    // UPDATED to use the correct BiometricManager class
    private lateinit var biometricManager: BiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UPDATED to use the correct BiometricManager class
        biometricManager = BiometricManager(this)

        setContent {
            SecureNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(false) }

                    if (isAuthenticated) {
                        NotesAppNavigation(noteViewModel)
                    } else {
                        // This prevents the auth prompt from appearing every time the screen rotates
                        LaunchedEffect(Unit) {
                            biometricManager.promptForAuthentication {
                                isAuthenticated = true
                            }
                        }
                        // Show a loading/locked screen while waiting for authentication
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
            val noteId = backStackEntry.arguments?.getInt("noteId")
            NoteEditScreen(
                navController = navController,
                viewModel = noteViewModel,
                noteId = if (noteId == -1) null else noteId
            )
        }
    }
}
