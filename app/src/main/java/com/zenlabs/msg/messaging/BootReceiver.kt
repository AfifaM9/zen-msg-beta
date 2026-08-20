package com.zenlabs.msg.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zenlabs.msg.data.ZenMsgDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * On boot, this is a no-op besides warming the database so the first incoming
 * SMS doesn't pay a cold-start penalty. Pending work can be hooked here later.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ZenMsgDatabase.get(context).openHelper.writableDatabase
            } catch (t: Throwable) {
                // ignore
            } finally {
                pendingResult.finish()
            }
        }
    }
}
