package com.questkeep.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.PlayerNote

@Entity(tableName = "player_notes")
data class PlayerNoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun PlayerNoteEntity.toDomain() = PlayerNote(
    id = id, userId = userId, title = title,
    content = content, createdAt = createdAt, updatedAt = updatedAt
)

fun PlayerNote.toEntity() = PlayerNoteEntity(
    id = id, userId = userId, title = title,
    content = content, createdAt = createdAt, updatedAt = updatedAt
)
