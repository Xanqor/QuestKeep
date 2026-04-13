package com.questkeep.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.Spell

@Entity(tableName = "spells")
data class SpellEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val name: String,

    // Xantheia recipe
    val coreId: String,
    val coreName: String,
    val modId: String,
    val modName: String,
    val expressionId: String,
    val expressionName: String,
    val accentsJson: String,
    val powerLevel: Int,
    val effectText: String,

    // Calculated costs
    val quarCost: Float,
    val ombraCost: Float,
    val verunaCost: Float,
    val sumeniCost: Float,

    val createdAt: Long
)

fun SpellEntity.toDomain() = Spell(
    id = id, characterId = characterId, name = name,
    coreId = coreId, coreName = coreName,
    modId = modId, modName = modName,
    expressionId = expressionId, expressionName = expressionName,
    accentsJson = accentsJson, powerLevel = powerLevel, effectText = effectText,
    quarCost = quarCost, ombraCost = ombraCost,
    verunaCost = verunaCost, sumeniCost = sumeniCost,
    createdAt = createdAt
)

fun Spell.toEntity() = SpellEntity(
    id = id, characterId = characterId, name = name,
    coreId = coreId, coreName = coreName,
    modId = modId, modName = modName,
    expressionId = expressionId, expressionName = expressionName,
    accentsJson = accentsJson, powerLevel = powerLevel, effectText = effectText,
    quarCost = quarCost, ombraCost = ombraCost,
    verunaCost = verunaCost, sumeniCost = sumeniCost,
    createdAt = createdAt
)
