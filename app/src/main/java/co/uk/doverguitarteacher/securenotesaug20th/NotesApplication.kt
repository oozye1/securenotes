package co.uk.doverguitarteacher.securenotesaug20th

import android.app.Application

class NotesApplication : Application() {
    // Using by lazy so the database and repository are only created when they're needed
    // rather than when the application starts.
    val database by lazy { DatabaseProvider.getInstance(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }
}
