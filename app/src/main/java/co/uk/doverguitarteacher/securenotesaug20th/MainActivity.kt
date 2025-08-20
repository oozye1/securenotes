package co.uk.doverguitarteacher.securenotesaug20th

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.uk.doverguitarteacher.securenotesaug20th.ui.theme.SecureNotesTheme

class MainActivity : ComponentActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as NotesApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "note_list") {
                        // The Main List Screen
                        composable("note_list") {
                            NoteListScreen(
                                viewModel = noteViewModel,
                                onAddNote = {
                                    // Go to the edit screen with no ID
                                    navController.navigate("note_edit")
                                },
                                onNoteClick = { note ->
                                    // Go to the edit screen WITH the note's ID
                                    navController.navigate("note_edit?noteId=${note.id}")
                                }
                            )
                        }

                        // The Add/Edit Screen
                        composable(
                            route = "note_edit?noteId={noteId}",
                            arguments = listOf(navArgument("noteId") {
                                type = NavType.IntType
                                defaultValue = -1 // Indicates a new note
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
            }
        }
    }
}
