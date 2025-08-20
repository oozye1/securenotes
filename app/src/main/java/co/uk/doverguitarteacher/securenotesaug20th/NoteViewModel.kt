package co.uk.doverguitarteacher.securenotesaug20th

import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    // --- NEW STATEFLOW FOR SORT ORDER ---
    private val _sortOrder = MutableStateFlow(SortOrder.BY_DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    // --- UPGRADED FLOW COMBINING SEARCH AND SORT ---
    val notes: StateFlow<List<Note>> =
        combine(_searchText, _sortOrder) { text, sort ->
            Pair(text, sort) // Emit a pair of the latest values
        }.flatMapLatest { (query, sort) ->
            if (query.isBlank()) {
                repository.getNotes(sort)
            } else {
                repository.searchNotes(query, sort)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }

    // --- NEW FUNCTION TO CHANGE SORT ORDER ---
    fun onSortOrderChange(newSortOrder: SortOrder) {
        _sortOrder.value = newSortOrder
    }


    // --- UNCHANGED FUNCTIONS ---
    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun deleteById(id: Int) = viewModelScope.launch {
        repository.deleteAndWipeById(id)
    }

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
