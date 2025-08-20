package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.Database
import androidx.room.RoomDatabase

// BUMP THE VERSION NUMBER FROM 3 to 4
@Database(entities = [Note::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
