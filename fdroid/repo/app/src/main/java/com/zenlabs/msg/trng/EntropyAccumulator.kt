package com.zenlabs.msg.trng

/**
 * Converts a (timestamp, random) pair into a compact, sortable, collision-free
 * message ID using Crockford base32. 26 base32 chars encode ~130 bits.
 */
internal class EntropyAccumulator {

    fun toMessageId(timestampMs: Long, random: ByteArray): String {
        // Pack: 48-bit millisecond ts (lower 6 bytes of epoch ms) + 80-bit random.
        val tsBytes = ByteArray(6)
        var ts = timestampMs
        for (i in 5 downTo 0) {
            tsBytes[i] = (ts and 0xFF).toByte()
            ts = ts ushr 8
        }
        val packed = tsBytes + random
        return encodeCrockford(packed)
    }

    private fun encodeCrockford(bytes: ByteArray): String {
        val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        val sb = StringBuilder()
        var buffer = 0L
        var bitsLeft = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toLong() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = ((buffer ushr (bitsLeft - 5)) and 0x1F).toInt()
                sb.append(alphabet[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = ((buffer shl (5 - bitsLeft)) and 0x1F).toInt()
            sb.append(alphabet[index])
        }
        return sb.toString()
    }
}
