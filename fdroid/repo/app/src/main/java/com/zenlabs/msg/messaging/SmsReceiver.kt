package com.zenlabs.msg.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.zenlabs.msg.data.ZenMsgDatabase
import com.zenlabs.msg.data.entity.Conversation
import com.zenlabs.msg.data.entity.Message
import com.zenlabs.msg.trng.Trng
import com.zenlabs.msg.notifications.ZenNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives incoming SMS via the system's SMS_RECEIVED broadcast. For every PDU,
 * it extracts the sender + body, persists a new [Message], updates (or creates)
 * the conversation, and posts a notification — exactly the lifecycle Google
 * Messages follows for an incoming text.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = ZenMsgDatabase.get(context)
                // Group PDUs by originating address — a long SMS may arrive as
                // multiple fragments that need concatenation.
                val grouped = messages.groupBy { it.displayOriginatingAddress ?: "" }
                for ((rawAddress, pdus) in grouped) {
                    val address = SmsAddress.normalize(rawAddress)
                    val body = pdus.joinToString("") { it.displayMessageBody ?: "" }
                    val timestamp = pdus.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                    val existing = db.conversationDao().getByAddress(address)
                    val conversation = if (existing != null) {
                        existing
                    } else {
                        val newId = db.conversationDao().insert(Conversation(address = address))
                        db.conversationDao().getById(newId) ?: Conversation(id = newId, address = address)
                    }

                    val trngId = Trng.nextMessageId(context)
                    val msg = Message(
                        conversationId = conversation.id,
                        address = address,
                        body = body,
                        timestamp = timestamp,
                        sentByMe = false,
                        status = Message.STATUS_DELIVERED,
                        trngId = trngId,
                        trngKeyHex = null
                    )
                    db.messageDao().insert(msg)

                    val updated = conversation.copy(
                        lastMessageBody = body,
                        lastMessageTimestamp = timestamp,
                        unreadCount = conversation.unreadCount + 1
                    )
                    db.conversationDao().update(updated)

                    ZenNotifications.notifyIncomingSms(context, updated, body)
                }
            } catch (t: Throwable) {
                // Swallow; we cannot crash the broadcast receiver.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
