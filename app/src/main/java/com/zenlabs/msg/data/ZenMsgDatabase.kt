package com.zenlabs.msg.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenlabs.msg.data.dao.ConversationDao
import com.zenlabs.msg.data.dao.MessageDao
import com.zenlabs.msg.data.entity.Conversation
import com.zenlabs.msg.data.entity.Message

@Database(
    entities = [Conversation::class, Message::class],
    version = 1,
    exportSchema = true
)
abstract class ZenMsgDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ZenMsgDatabase? = null

        fun get(context: Context): ZenMsgDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZenMsgDatabase::class.java,
                    "zenmsg.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
