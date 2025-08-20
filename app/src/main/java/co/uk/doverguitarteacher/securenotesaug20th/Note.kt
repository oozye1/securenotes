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
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val salt: ByteArray? = null,
    val createdAt: Long,
    val updatedAt: Long,

    // ADD THIS NEW FIELD
    val imageFilename: String? = null
) {
    // Boilerplate for byte array comparison is needed when it's a field
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
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (imageFilename != other.imageFilename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + (salt?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + (imageFilename?.hashCode() ?: 0)
        return result
    }
}
