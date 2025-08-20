// CHANGE THIS LINE
package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.Entity
import androidx.room.PrimaryKey

// CHANGE THIS LINE to match your DAO query
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String
)
