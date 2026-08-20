package com.zenlabs.msg.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zenlabs.msg.MainActivity
import com.zenlabs.msg.data.entity.Conversation

/**
 * Posts incoming-message notifications. Mirrors the minimum a real messaging
 * client needs: a channel, a content title, body preview, and a tap action
 * that opens the conversation.
 */
object ZenNotifications {

    private const val CHANNEL_ID = "zenmsg_messages"
    private const val CHANNEL_NAME = "Messages"
    private var channelCreated = false

    fun notifyIncomingSms(context: Context, conversation: Conversation, body: String) {
        ensureChannel(context)
        val title = conversation.contactName ?: conversation.address
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversation.id)
        }
        val pi = PendingIntent.getActivity(
            context,
            conversation.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(conversation.id.toInt(), notif)
    }

    private fun ensureChannel(context: Context) {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming SMS messages"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        channelCreated = true
    }
}
