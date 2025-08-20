package co.uk.doverguitarteacher.securenotesaug20th

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNoteById(id: Int): Flow<Note> {
        return noteDao.getNoteById(id)
    }

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteAndWipeById(id: Int) {
        noteDao.overwriteNoteById(id)
        noteDao.deleteNoteById(id)
    }
}
