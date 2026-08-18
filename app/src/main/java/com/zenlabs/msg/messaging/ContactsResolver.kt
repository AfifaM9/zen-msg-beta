package com.zenlabs.msg.messaging

import android.content.Context
import android.provider.ContactsContract

/**
 * Resolves a normalized phone number to a display name from the device's
 * contacts provider. Returns null if no match or permission missing.
 */
object ContactsResolver {

    fun lookupName(context: Context, address: String): String? {
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val selection = "${ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER} = ?"
            val normalized = PhoneNumberUtils.normalizeNumber(address)
            context.contentResolver.query(
                uri, projection, selection, arrayOf(normalized), null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (t: SecurityException) {
            null
        } catch (t: Throwable) {
            null
        }
    }
}

private object PhoneNumberUtils {
    fun normalizeNumber(phone: String): String {
        // Mirror android.telephony.PhoneNumberUtils.normalizeNumber's default
        // behaviour: strip everything that isn't a keypad digit/plus.
        val sb = StringBuilder()
        for (ch in phone) {
            when {
                ch in '0'..'9' -> sb.append(ch)
                ch == '+' && sb.isEmpty() -> sb.append(ch)
                ch in 'A'..'Z' -> sb.append(charToKeypad(ch))
                ch in 'a'..'z' -> sb.append(charToKeypad(ch.uppercaseChar()))
            }
        }
        return sb.toString()
    }

    private fun charToKeypad(c: Char): Char = when (c) {
        'A', 'B', 'C' -> '2'
        'D', 'E', 'F' -> '3'
        'G', 'H', 'I' -> '4'
        'J', 'K', 'L' -> '5'
        'M', 'N', 'O' -> '6'
        'P', 'Q', 'R', 'S' -> '7'
        'T', 'U', 'V' -> '8'
        'W', 'X', 'Y', 'Z' -> '9'
        else -> c
    }
}
