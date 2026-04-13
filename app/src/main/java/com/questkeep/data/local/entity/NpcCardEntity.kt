package com.questkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.NpcCard

@Entity(tableName = "npc_cards")
data class NpcCardEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val name: String,
    val description: String,
    @ColumnInfo(defaultValue = "0") val hp: Int,
    @ColumnInfo(defaultValue = "0") val armor: Int,
    @ColumnInfo(defaultValue = "") val damage: String,
    @ColumnInfo(defaultValue = "[]") val tagsJson: String,
    @ColumnInfo(defaultValue = "{}") val statsJson: String,
    val createdAt: Long
) {
    fun toDomain() = NpcCard(
        id = id, campaignId = campaignId,
        name = name, description = description,
        hp = hp, armor = armor, damage = damage,
        tags = tagsJson.removePrefix("[").removeSuffix("]")
            .split(",").map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() },
        statsJson = statsJson, createdAt = createdAt
    )
}

fun NpcCard.toEntity() = NpcCardEntity(
    id = id, campaignId = campaignId,
    name = name, description = description,
    hp = hp, armor = armor, damage = damage,
    tagsJson = "[${tags.joinToString(",") { "\"$it\"" }}]",
    statsJson = statsJson, createdAt = createdAt
)
