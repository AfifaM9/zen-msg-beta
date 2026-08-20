package com.zenlabs.msg.messaging

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.zenlabs.msg.data.ZenMsgDatabase
import com.zenlabs.msg.data.entity.Conversation
import com.zenlabs.msg.data.entity.Message
import com.zenlabs.msg.trng.Trng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sends SMS for real, using [SmsManager]. For each message it:
 *  - generates a TRNG id + nonce,
 *  - inserts a PENDING row into the database,
 *  - registers sent + delivery PendingIntents whose broadcasts are handled by
 *    [SmsDeliveryReceiver],
 *  - calls [SmsManager.sendTextMessage].
 *
 * On a real device with a SIM and the SEND_SMS permission granted, this places
 * an actual SMS on the carrier network — same primitive Google Messages uses.
 */
class SmsSender(private val context: Context) {

    private val db = ZenMsgDatabase.get(context)

    suspend fun send(address: String, body: String): Long = withContext(Dispatchers.IO) {
        val normalized = SmsAddress.normalize(address)
        val conversation = ensureConversation(normalized)

        val now = System.currentTimeMillis()
        val trngId = Trng.nextMessageId(context)
        val trngKey = Trng.nextKey(context).toHex()

        val pending = Message(
            conversationId = conversation.id,
            address = normalized,
            body = body,
            timestamp = now,
            sentByMe = true,
            status = Message.STATUS_PENDING,
            trngId = trngId,
            trngKeyHex = trngKey
        )
        val rowId = db.messageDao().insert(pending)

        // Update conversation preview.
        db.conversationDao().update(
            conversation.copy(
                lastMessageBody = body,
                lastMessageTimestamp = now,
                draft = null
            )
        )

        val smsManager = resolveSmsManager()
        val sentIntent = deliveryPendingIntent(SmsDeliveryReceiver.ACTION_SENT, rowId, normalized)
        val deliveredIntent = deliveryPendingIntent(SmsDeliveryReceiver.ACTION_DELIVERED, rowId, normalized)

        try {
            smsManager.sendTextMessage(
                /* destinationAddress = */ normalized,
                /* scAddress = */ null,
                /* text = */ body,
                /* sentIntent = */ sentIntent,
                /* deliveryIntent = */ deliveredIntent
            )
        } catch (t: Throwable) {
            db.messageDao().updateStatus(rowId, Message.STATUS_FAILED)
        }
        rowId
    }

    private suspend fun ensureConversation(address: String): Conversation {
        val existing = db.conversationDao().getByAddress(address)
        if (existing != null) return existing
        val id = db.conversationDao().insert(Conversation(address = address))
        return db.conversationDao().getById(id) ?: Conversation(id = id, address = address)
    }

    private fun resolveSmsManager(): SmsManager {
        // On API 31+, prefer the per-subscription SmsManager for the default SIM.
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
                .let { it.takeIf { sm -> sm != null } }
                ?: SmsManager.getSmsManagerForSubscriptionId(SmsManager.getDefaultSmsSubscriptionId())
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    private fun deliveryPendingIntent(action: String, rowId: Long, address: String): PendingIntent {
        val intent = Intent(context, SmsDeliveryReceiver::class.java).apply {
            this.action = action
            putExtra(SmsDeliveryReceiver.EXTRA_ROW_ID, rowId)
            putExtra(SmsDeliveryReceiver.EXTRA_ADDRESS, address)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, rowId.toInt(), intent, flags)
    }
}

private fun ByteArray.toHex(): String =
    joinToString("") { "%02x".format(it) }
