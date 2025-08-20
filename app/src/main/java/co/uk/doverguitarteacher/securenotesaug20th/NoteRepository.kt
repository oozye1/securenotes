package co.uk.doverguitarteacher.securenotesaug20th

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first // <-- IMPORT THAT FIXES THE '.first()' ERROR
import kotlinx.coroutines.withContext

class NoteRepository(
    private val noteDao: NoteDao,
    private val context: Context
) {

    fun getNotes(sortOrder: SortOrder): Flow<List<Note>> {
        return when (sortOrder) {
            SortOrder.BY_DATE_DESC -> noteDao.getNotesSortedByDate()
            SortOrder.BY_TITLE_ASC -> noteDao.getNotesSortedByTitle()
        }
    }

    fun searchNotes(query: String, sortOrder: SortOrder): Flow<List<Note>> {
        return when (sortOrder) {
            SortOrder.BY_DATE_DESC -> noteDao.searchNotesSortedByDate(query)
            SortOrder.BY_TITLE_ASC -> noteDao.searchNotesSortedByTitle(query)
        }
    }

    suspend fun deleteAndWipeById(id: Int) {
        withContext(Dispatchers.IO) {
            val noteToDelete = noteDao.getNoteById(id).first() // This now works
            noteToDelete.imageFilename?.let { filename ->
                context.deleteFile(filename)
            }
            noteDao.overwriteNoteById(id)
            noteDao.deleteNoteById(id)
        }
    }

    fun getNoteById(id: Int): Flow<Note> {
        return noteDao.getNoteById(id)
    }

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }
}
