package co.uk.doverguitarteacher.securenotesaug20th

import android.app.Application

class NotesApplication : Application() {
    val database by lazy { DatabaseProvider.getInstance(this) }
    // Pass the application context to the repository
    val repository by lazy { NoteRepository(noteDao = database.noteDao(), context = this) }
}
