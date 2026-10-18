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
    @ColumnInfo(defaultValue = "0")   val armorBlock: Int = 0,
    @ColumnInfo(defaultValue = "0")   val constitution: Int = 0,
    val movement: Int,
    @ColumnInfo(defaultValue = "0")   val perception: Int = 0,
    @ColumnInfo(defaultValue = "0")   val intelligence: Int = 0,
    @ColumnInfo(defaultValue = "0")   val influence: Int = 0,
    @ColumnInfo(defaultValue = "0")   val willpower: Int = 0,
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
        armor = armor, maxArmor = maxArmor, armorBlock = armorBlock,
        constitution = constitution, movement = movement, perception = perception,
        intelligence = intelligence, influence = influence, willpower = willpower,
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
    armor = armor, maxArmor = maxArmor, armorBlock = armorBlock,
    constitution = constitution, movement = movement, perception = perception,
    intelligence = intelligence, influence = influence, willpower = willpower,
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
