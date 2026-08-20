package com.zenlabs.msg.data

import android.content.Context
import com.zenlabs.msg.data.entity.Conversation
import com.zenlabs.msg.data.entity.Message
import com.zenlabs.msg.messaging.ContactsResolver
import com.zenlabs.msg.messaging.SmsAddress
import com.zenlabs.msg.messaging.SmsSender
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth between the UI and the data + SMS layers. The UI talks
 * only to this; it never touches Room or [SmsSender] directly.
 */
class ZenRepository(private val context: Context) {

    private val db = ZenMsgDatabase.get(context)
    private val sender = SmsSender(context)

    fun observeConversations(): Flow<List<Conversation>> =
        db.conversationDao().observeAll()

    fun observeConversation(id: Long): Flow<Conversation?> =
        db.conversationDao().observeById(id)

    fun observeMessages(conversationId: Long): Flow<List<Message>> =
        db.messageDao().observeByConversation(conversationId)

    suspend fun sendMessage(address: String, body: String): Long =
        sender.send(address, body)

    suspend fun markRead(conversationId: Long) =
        db.conversationDao().clearUnread(conversationId)

    suspend fun deleteConversation(conversationId: Long) {
        db.messageDao().deleteByConversation(conversationId)
        db.conversationDao().delete(conversationId)
    }

    suspend fun deleteMessage(messageId: Long) =
        db.messageDao().delete(messageId)

    suspend fun togglePin(conversationId: Long, pinned: Boolean) =
        db.conversationDao().setPinned(conversationId, pinned)

    suspend fun toggleArchive(conversationId: Long, archived: Boolean) =
        db.conversationDao().setArchived(conversationId, archived)

    suspend fun toggleBlock(conversationId: Long, blocked: Boolean) =
        db.conversationDao().setBlocked(conversationId, blocked)

    suspend fun saveDraft(conversationId: Long, draft: String?) =
        db.conversationDao().setDraft(conversationId, draft)

    suspend fun resolveContactName(address: String): String? =
        ContactsResolver.lookupName(context, SmsAddress.normalize(address))

    suspend fun ensureConversationId(address: String): Long {
        val normalized = SmsAddress.normalize(address)
        val existing = db.conversationDao().getByAddress(normalized)
        if (existing != null) return existing.id
        val name = ContactsResolver.lookupName(context, normalized)
        val id = db.conversationDao().insert(Conversation(address = normalized, contactName = name))
        return id
    }
}
