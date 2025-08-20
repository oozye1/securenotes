package co.uk.doverguitarteacher.securenotesaug20th

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    // --- UPDATED LOGIC TO HANDLE SORTING ---
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

    // --- UNCHANGED FUNCTIONS ---
    fun getNoteById(id: Int): Flow<Note> {
        return noteDao.getNoteById(id)
    }

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteAndWipeById(id: Int) {
        noteDao.overwriteNoteById(id)
        noteDao.deleteNoteById(id)
    }
}
