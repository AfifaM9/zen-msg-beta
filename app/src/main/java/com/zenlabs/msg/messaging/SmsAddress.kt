package com.zenlabs.msg.messaging

import android.telephony.PhoneNumberUtils

/**
 * Normalizes addresses so that one phone number maps to exactly one
 * conversation regardless of formatting differences ("+1 (555) 0142" vs
 * "5550142"). Falls back to a trimmed string for non-numeric handles.
 */
object SmsAddress {

    fun normalize(address: String): String {
        val cleaned = address.trim()
        if (cleaned.isEmpty()) return cleaned
        // If it looks like a phone number, use the platform normalizer; this
        // also handles country codes and formatting separators.
        return if (PhoneNumberUtils.isWellFormedSmsAddress(cleaned)) {
            PhoneNumberUtils.stripSeparators(cleaned)
        } else {
            cleaned.lowercase()
        }
    }
}
