package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val isEncrypted: Boolean = false,

    // ADD THIS FIELD to store the unique salt for each note
    // It's a byte array, which Room knows how to store as a BLOB
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val salt: ByteArray? = null
) {
    // These equals/hashCode functions are important when working with byte arrays in data classes
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (isEncrypted != other.isEncrypted) return false
        if (salt != null) {
            if (other.salt == null) return false
            if (!salt.contentEquals(other.salt)) return false
        } else if (other.salt != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + (salt?.contentHashCode() ?: 0)
        return result
    }
}
