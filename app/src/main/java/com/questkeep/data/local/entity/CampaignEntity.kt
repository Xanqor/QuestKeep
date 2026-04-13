package com.questkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.Campaign

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val gameSystem: String,
    val maxPlayers: Int,
    val gmId: String,
    val inviteCode: String,
    val playerIdsJson: String,      // JSON array — only actual players
    @ColumnInfo(defaultValue = "[]") val coGmIdsJson: String,
    @ColumnInfo(defaultValue = "[]") val allMemberIdsJson: String,
    val createdAt: Long
) {
    fun toDomain() = Campaign(
        id = id, name = name, description = description,
        gameSystem = gameSystem, maxPlayers = maxPlayers,
        gmId = gmId, inviteCode = inviteCode,
        playerIds = parseJsonArray(playerIdsJson),
        coGmIds = parseJsonArray(coGmIdsJson),
        allMemberIds = parseJsonArray(allMemberIdsJson),
        createdAt = createdAt
    )

    private fun parseJsonArray(json: String): List<String> =
        json.removePrefix("[").removeSuffix("]")
            .split(",").map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
}

fun Campaign.toEntity() = CampaignEntity(
    id = id, name = name, description = description,
    gameSystem = gameSystem, maxPlayers = maxPlayers,
    gmId = gmId, inviteCode = inviteCode,
    playerIdsJson = jsonArray(playerIds),
    coGmIdsJson = jsonArray(coGmIds),
    allMemberIdsJson = jsonArray(allMemberIds),
    createdAt = createdAt
)

private fun jsonArray(list: List<String>) = "[${list.joinToString(",") { "\"$it\"" }}]"
