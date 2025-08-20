package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.Database
import androidx.room.RoomDatabase

// ADD exportSchema = false
@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
