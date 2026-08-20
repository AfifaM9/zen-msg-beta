package com.zenlabs.msg.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zenlabs.msg.data.ZenRepository
import com.zenlabs.msg.data.entity.Conversation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val repo: ZenRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> =
        repo.observeConversations().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteConversation(id: Long) = viewModelScope.launch { repo.deleteConversation(id) }
    fun togglePin(id: Long, pinned: Boolean) = viewModelScope.launch { repo.togglePin(id, pinned) }
    fun toggleArchive(id: Long, archived: Boolean) = viewModelScope.launch { repo.toggleArchive(id, archived) }

    companion object {
        fun factory(repo: ZenRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ConversationListViewModel(repo) as T
        }
    }
}
