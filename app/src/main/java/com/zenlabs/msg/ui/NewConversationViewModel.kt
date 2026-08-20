package com.zenlabs.msg.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zenlabs.msg.data.ZenRepository
import com.zenlabs.msg.messaging.SmsAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewConversationViewModel(private val repo: ZenRepository) : ViewModel() {

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _createdId = MutableStateFlow<Long?>(null)
    val createdId: StateFlow<Long?> = _createdId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setAddress(value: String) {
        _address.value = value
        _error.value = null
    }

    fun create() {
        val raw = _address.value.trim()
        if (raw.isEmpty()) {
            _error.value = "Enter a phone number"
            return
        }
        val normalized = SmsAddress.normalize(raw)
        if (normalized.isEmpty()) {
            _error.value = "Invalid phone number"
            return
        }
        viewModelScope.launch {
            // Send an empty seed is not desirable; instead, just create the
            // conversation by sending nothing. We create a placeholder row only
            // when the first real message is sent. To open the chat immediately,
            // we resolve-or-create by inserting a conversation row.
            val existing = repo.observeConversations() // not used directly
            _createdId.value = repo.ensureConversationId(normalized)
        }
    }

    companion object {
        fun factory(repo: ZenRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NewConversationViewModel(repo) as T
        }
    }
}
