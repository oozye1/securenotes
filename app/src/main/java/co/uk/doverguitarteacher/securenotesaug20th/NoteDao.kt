package co.uk.doverguitarteacher.securenotesaug20th

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Existing queries
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getNotesSortedByDate(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Flow<Note>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchNotesSortedByDate(query: String): Flow<List<Note>>

    // --- ADD THESE NEW QUERIES FOR TITLE SORTING ---
    @Query("SELECT * FROM notes ORDER BY title ASC")
    fun getNotesSortedByTitle(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchNotesSortedByTitle(query: String): Flow<List<Note>>


    // --- UNCHANGED FUNCTIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("UPDATE notes SET title = 'DELETED', content = '', isEncrypted = 0, salt = NULL WHERE id = :id")
    suspend fun overwriteNoteById(id: Int)
}
