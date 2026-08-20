package com.zenlabs.msg.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zenlabs.msg.data.entity.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeByConversation(conversationId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE trngId = :trngId LIMIT 1")
    suspend fun getByTrngId(trngId: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND sentByMe = 0 AND status = ${Message.STATUS_DELIVERED}")
    fun countUnread(conversationId: Long): Flow<Int>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long)
}
