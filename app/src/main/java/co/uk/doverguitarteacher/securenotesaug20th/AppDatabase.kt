package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.Database
import androidx.room.RoomDatabase

// BUMP THE VERSION NUMBER FROM 1 to 2
@Database(entities = [Note::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
