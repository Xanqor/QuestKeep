package com.questkeep.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.questkeep.data.local.entity.*

@Database(
    entities = [
        CharacterEntity::class,
        MessageEntity::class,
        CampaignEntity::class,
        GmNoteEntity::class,
        NpcCardEntity::class,
        SpellEntity::class,
        InventoryItemEntity::class,
        ItemCardEntity::class,
        SharedInventoryItemEntity::class,
        PlayerNoteEntity::class,
        PlayerNpcEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class QuestKeepDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun messageDao(): MessageDao
    abstract fun campaignDao(): CampaignDao
    abstract fun gmNoteDao(): GmNoteDao
    abstract fun npcCardDao(): NpcCardDao
    abstract fun spellDao(): SpellDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun itemCardDao(): ItemCardDao
    abstract fun sharedInventoryDao(): SharedInventoryDao
    abstract fun playerNoteDao(): PlayerNoteDao
    abstract fun playerNpcDao(): PlayerNpcDao
}
