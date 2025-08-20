package co.uk.doverguitarteacher.securenotesaug20th

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(
    private val repository: NoteRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- STATE FOR THE LIST SCREEN (Unchanged) ---
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()
    private val _sortOrder = MutableStateFlow(SortOrder.BY_DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()
    val notes: StateFlow<List<Note>> =
        combine(_searchText, _sortOrder) { text, sort -> Pair(text, sort) }
            .flatMapLatest { (query, sort) ->
                if (query.isBlank()) repository.getNotes(sort) else repository.searchNotes(query, sort)
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // --- STATE FOR THE EDIT SCREEN (Now with tempCameraUri) ---
    val editNoteTitle: StateFlow<String> = savedStateHandle.getStateFlow("editNoteTitle", "")
    val editNoteContent: StateFlow<String> = savedStateHandle.getStateFlow("editNoteContent", "")
    val editNoteImageUri: StateFlow<Uri?> = savedStateHandle.getStateFlow("editNoteImageUri", null)

    // --- THIS IS THE CRITICAL FIX ---
    // This holds the URI for the camera photo and WILL SURVIVE process death.
    val tempCameraImageUri: StateFlow<Uri?> = savedStateHandle.getStateFlow("tempCameraImageUri", null)

    val editNoteExistingData = MutableStateFlow<Note?>(null)

    fun loadNoteForEdit(noteId: Int?) {
        if (noteId == null) {
            savedStateHandle["editNoteTitle"] = ""
            savedStateHandle["editNoteContent"] = ""
            savedStateHandle["editNoteImageUri"] = null
            editNoteExistingData.value = null
        } else {
            viewModelScope.launch {
                val note = repository.getNoteById(noteId).first()
                editNoteExistingData.value = note
                if (note != null) {
                    savedStateHandle["editNoteTitle"] = note.title
                    savedStateHandle["editNoteContent"] = note.content
                }
            }
        }
    }

    /**
     * Creates a temporary URI for the camera, saves it to the SavedStateHandle so it survives process death,
     * and returns it to the UI to be used by the camera launcher.
     */
    fun createTempCameraUri(context: Context): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
        savedStateHandle["tempCameraImageUri"] = uri
        return uri
    }

    fun onTitleChange(newTitle: String) { savedStateHandle["editNoteTitle"] = newTitle }
    fun onContentChange(newContent: String) { savedStateHandle["editNoteContent"] = newContent }
    fun onImageUriChange(newUri: Uri?) { savedStateHandle["editNoteImageUri"] = newUri }
    fun onSearchTextChanged(text: String) { _searchText.value = text }
    fun onSortOrderChange(newSortOrder: SortOrder) { _sortOrder.value = newSortOrder }
    fun insert(note: Note) = viewModelScope.launch { repository.insert(note) }
    fun update(note: Note) = viewModelScope.launch { repository.update(note) }
    fun deleteById(id: Int) = viewModelScope.launch { repository.deleteAndWipeById(id) }
    fun getNoteById(id: Int): LiveData<Note> = repository.getNoteById(id).asLiveData()
}


class NoteViewModelFactory(
    private val repository: NoteRepository,
    owner: androidx.savedstate.SavedStateRegistryOwner,
    defaultArgs: android.os.Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
