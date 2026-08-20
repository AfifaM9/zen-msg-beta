package com.zenlabs.msg.messaging

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zenlabs.msg.data.ZenMsgDatabase
import com.zenlabs.msg.data.entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the SENT and DELIVERED PendingIntents registered by [SmsSender].
 * Updates the matching message row's status so the UI reflects reality.
 */
class SmsDeliveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val rowId = intent.getLongExtra(EXTRA_ROW_ID, -1L)
        if (rowId < 0) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ZenMsgDatabase.get(context).messageDao()
                when (intent.action) {
                    ACTION_SENT -> {
                        val ok = resultCode == Activity.RESULT_OK
                        dao.updateStatus(rowId, if (ok) Message.STATUS_SENT else Message.STATUS_FAILED)
                    }
                    ACTION_DELIVERED -> {
                        if (resultCode == Activity.RESULT_OK) {
                            dao.updateStatus(rowId, Message.STATUS_DELIVERED)
                        }
                    }
                }
            } catch (t: Throwable) {
                // ignore
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SENT = "com.zenlabs.msg.SMS_SENT"
        const val ACTION_DELIVERED = "com.zenlabs.msg.SMS_DELIVERED"
        const val EXTRA_ROW_ID = "row_id"
        const val EXTRA_ADDRESS = "address"
    }
}
