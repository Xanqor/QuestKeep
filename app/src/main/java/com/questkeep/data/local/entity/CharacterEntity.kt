package com.questkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.Character

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val className: String,
    val level: Int,
    val campaignId: String,
    val playerId: String,
    val currentHp: Int,
    val maxHp: Int,
    val armor: Int,
    @ColumnInfo(defaultValue = "0")   val maxArmor: Int,
    val wellbeing: Int,
    val movement: Int,
    val combat: Int,
    val thinking: Int,
    val attention: Int,
    val communication: Int,
    val damage: Int,
    val dodge: Int,
    val initiative: Int,
    val quarResource: Float,
    val ombraResource: Float,
    val verunaResource: Float,
    val sumeniResource: Float,
    @ColumnInfo(defaultValue = "4.0") val maxQuarResource: Float,
    @ColumnInfo(defaultValue = "4.0") val maxOmbraResource: Float,
    @ColumnInfo(defaultValue = "4.0") val maxVerunaResource: Float,
    @ColumnInfo(defaultValue = "4.0") val maxSumeniResource: Float,
    val inspirationPoints: Int,
    @ColumnInfo(defaultValue = "0")   val experiencePoints: Int,
    @ColumnInfo(defaultValue = "0")   val crowns: Int,
    val updatedAt: Long
) {
    fun toDomain() = Character(
        id = id, name = name, className = className, level = level,
        campaignId = campaignId, playerId = playerId,
        currentHp = currentHp, maxHp = maxHp,
        armor = armor, maxArmor = maxArmor,
        wellbeing = wellbeing, movement = movement, combat = combat,
        thinking = thinking, attention = attention, communication = communication,
        damage = damage, dodge = dodge, initiative = initiative,
        quarResource = quarResource, ombraResource = ombraResource,
        verunaResource = verunaResource, sumeniResource = sumeniResource,
        maxQuarResource = maxQuarResource, maxOmbraResource = maxOmbraResource,
        maxVerunaResource = maxVerunaResource, maxSumeniResource = maxSumeniResource,
        inspirationPoints = inspirationPoints,
        experiencePoints = experiencePoints,
        crowns = crowns,
        updatedAt = updatedAt
    )
}

fun Character.toEntity() = CharacterEntity(
    id = id, name = name, className = className, level = level,
    campaignId = campaignId, playerId = playerId,
    currentHp = currentHp, maxHp = maxHp,
    armor = armor, maxArmor = maxArmor,
    wellbeing = wellbeing, movement = movement, combat = combat,
    thinking = thinking, attention = attention, communication = communication,
    damage = damage, dodge = dodge, initiative = initiative,
    quarResource = quarResource, ombraResource = ombraResource,
    verunaResource = verunaResource, sumeniResource = sumeniResource,
    maxQuarResource = maxQuarResource, maxOmbraResource = maxOmbraResource,
    maxVerunaResource = maxVerunaResource, maxSumeniResource = maxSumeniResource,
    inspirationPoints = inspirationPoints,
    experiencePoints = experiencePoints,
    crowns = crowns,
    updatedAt = updatedAt
)
