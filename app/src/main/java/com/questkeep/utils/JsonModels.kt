package com.questkeep.utils

import com.questkeep.domain.entities.GmNote
import com.questkeep.domain.entities.ItemCard
import com.questkeep.domain.entities.NpcCard
import com.questkeep.domain.entities.Spell

data class SpellsExport(val version: Int = 1, val spells: List<Spell> = emptyList())
data class NpcsExport(val version: Int = 1, val npcs: List<NpcCard> = emptyList())
data class NotesExport(val version: Int = 1, val notes: List<GmNote> = emptyList())
data class ItemCardsExport(val version: Int = 1, val itemCards: List<ItemCard> = emptyList())
