package com.questkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.GmNote
import com.questkeep.domain.entities.NoteType

@Entity(tableName = "gm_notes")
data class GmNoteEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    @ColumnInfo(defaultValue = "") val parentId: String,  // "" = root
    val title: String,
    val content: String,
    val type: String,      // NoteType.name
    @ColumnInfo(defaultValue = "0") val isRevealedToPlayers: Int,  // 0 or 1
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = GmNote(
        id = id, campaignId = campaignId,
        parentId = parentId.ifEmpty { null },
        title = title, content = content,
        type = NoteType.entries.find { it.name == type } ?: NoteType.OTHER,
        isRevealedToPlayers = isRevealedToPlayers != 0,
        createdAt = createdAt, updatedAt = updatedAt
    )
}

fun GmNote.toEntity() = GmNoteEntity(
    id = id, campaignId = campaignId,
    parentId = parentId ?: "",
    title = title, content = content,
    type = type.name,
    isRevealedToPlayers = if (isRevealedToPlayers) 1 else 0,
    createdAt = createdAt, updatedAt = updatedAt
)
