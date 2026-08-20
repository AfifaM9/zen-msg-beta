package com.zenlabs.msg

import android.app.Application

/**
 * Application entry point. Kept minimal: database is lazily initialized by its
 * own singleton. Future global state (DI, logging) lives here.
 */
class ZenMsgApp : Application()
