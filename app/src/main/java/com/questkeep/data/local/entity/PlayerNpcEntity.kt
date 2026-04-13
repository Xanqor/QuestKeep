package com.questkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.PlayerNpc

@Entity(tableName = "player_npcs")
data class PlayerNpcEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val name: String,
    val description: String,
    @ColumnInfo(defaultValue = "0") val hp: Int,
    @ColumnInfo(defaultValue = "0") val armor: Int,
    @ColumnInfo(defaultValue = "") val damage: String,
    @ColumnInfo(defaultValue = "[]") val tagsJson: String,
    val createdAt: Long
) {
    fun toDomain() = PlayerNpc(
        id = id, campaignId = campaignId,
        name = name, description = description,
        hp = hp, armor = armor, damage = damage,
        tags = tagsJson.removePrefix("[").removeSuffix("]")
            .split(",").map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() },
        createdAt = createdAt
    )
}

fun PlayerNpc.toEntity() = PlayerNpcEntity(
    id = id, campaignId = campaignId,
    name = name, description = description,
    hp = hp, armor = armor, damage = damage,
    tagsJson = "[${tags.joinToString(",") { "\"$it\"" }}]",
    createdAt = createdAt
)
