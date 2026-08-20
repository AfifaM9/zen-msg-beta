package com.zenlabs.msg.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A conversation thread: one per address (phone number / normalized). The
 * [address] is normalized via [android.telephony.PhoneNumberUtils].
 */
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,            // normalized phone number or SIP-ish handle
    val contactName: String? = null,// resolved contact name (null => show address)
    val lastMessageBody: String = "",
    val lastMessageTimestamp: Long = 0,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val draft: String? = null,
    val blocked: Boolean = false
)
