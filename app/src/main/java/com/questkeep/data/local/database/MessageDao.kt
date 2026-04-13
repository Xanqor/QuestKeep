package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("""
        SELECT * FROM messages
        WHERE campaignId = :campaignId
        AND (isPublic = 1 OR senderId = :userId OR targetPlayerId = :userId)
        ORDER BY createdAt ASC
        LIMIT 100
    """)
    fun getMessagesForCampaign(campaignId: String, userId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE campaignId = :campaignId AND createdAt < :olderThan")
    suspend fun pruneOldMessages(campaignId: String, olderThan: Long)
}
