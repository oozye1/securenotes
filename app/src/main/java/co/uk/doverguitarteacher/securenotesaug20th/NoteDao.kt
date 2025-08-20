package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>

    // THIS IS THE CORRECTED QUERY
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Flow<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // This is the function for the secure wipe feature
    @Query("UPDATE notes SET title = 'DELETED', content = '', isEncrypted = 0, salt = NULL WHERE id = :id")
    suspend fun overwriteNoteById(id: Int)
}
