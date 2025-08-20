package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// BUMP THE VERSION NUMBER FROM 2 to 3
@Database(entities = [Note::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
