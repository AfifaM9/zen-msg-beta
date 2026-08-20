package com.zenlabs.msg.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zenlabs.msg.data.ZenRepository
import com.zenlabs.msg.data.entity.Conversation
import com.zenlabs.msg.data.entity.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: ZenRepository,
    private val conversationId: Long
) : ViewModel() {

    val conversation: StateFlow<Conversation?> =
        repo.observeConversation(conversationId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<Message>> =
        repo.observeMessages(conversationId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    init {
        viewModelScope.launch { repo.markRead(conversationId) }
    }

    fun setDraft(text: String) {
        _draft.value = text
    }

    fun send(onError: (String) -> Unit = {}) {
        val body = _draft.value.trim()
        if (body.isEmpty()) return
        val conversation = conversation.value ?: return onError("Conversation not loaded")
        viewModelScope.launch {
            try {
                repo.sendMessage(conversation.address, body)
                _draft.value = ""
                repo.saveDraft(conversationId, null)
            } catch (t: Throwable) {
                onError(t.message ?: "Failed to send")
            }
        }
    }

    fun saveDraft() {
        val text = _draft.value
        if (text.isNotBlank()) {
            viewModelScope.launch { repo.saveDraft(conversationId, text) }
        }
    }

    fun deleteMessage(id: Long) = viewModelScope.launch { repo.deleteMessage(id) }

    companion object {
        fun factory(repo: ZenRepository, conversationId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repo, conversationId) as T
        }
    }
}
