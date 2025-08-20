package co.uk.doverguitarteacher.securenotesaug20th

import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    // A private StateFlow to hold the search text
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    // This is a powerful feature: it combines the search text flow
    // with the database results. Every time the search text changes,
    // this code re-runs to get the filtered list.
    val notes: StateFlow<List<Note>> = searchText
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allNotes
            } else {
                repository.searchNotes(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Stay active for 5s after UI stops observing
            initialValue = emptyList()
        )

    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun deleteById(id: Int) = viewModelScope.launch {
        repository.deleteAndWipeById(id)
    }

    // This function remains, using LiveData is fine for one-shot reads like this.
    fun getNoteById(id: Int): LiveData<Note> {
        return repository.getNoteById(id).asLiveData()
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
