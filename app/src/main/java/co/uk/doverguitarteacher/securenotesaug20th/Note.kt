package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    // ADD THIS FIELD - default to false (not encrypted)
    val isEncrypted: Boolean = false
)
