package com.questkeep.domain.entities

data class Character(
    val id: String = "",
    val name: String = "",
    val className: String = "",
    val level: Int = 1,
    val campaignId: String = "",
    val playerId: String = "",

    // HP
    val currentHp: Int = 10,
    val maxHp: Int = 10,

    // Armor: Durability (current / max) & Flat Damage Reduction (Block)
    val armor: Int = 0,
    val maxArmor: Int = 0,
    val armorBlock: Int = 0,

    // 6 main stats (custom system)
    val constitution: Int = 0,  // Телосложение
    val movement: Int = 0,      // Движение
    val perception: Int = 0,    // Внимательность
    val intelligence: Int = 0,  // Интеллект
    val influence: Int = 0,     // Влияние
    val willpower: Int = 0,     // Воля

    // Combat parameters
    val damage: Int = 0,
    val dodge: Int = 0,
    val initiative: Int = 0,

    // Magic resources — current value (steps: 0, 0.5, 1, 1.5 … maxXxx)
    val quarResource: Float = 0f,    // Куар
    val ombraResource: Float = 0f,   // Омбра
    val verunaResource: Float = 0f,  // Веруна
    val sumeniResource: Float = 0f,  // Сумени

    // Magic resources — max (set by GM; default 4 units each)
    val maxQuarResource: Float = 4f,
    val maxOmbraResource: Float = 4f,
    val maxVerunaResource: Float = 4f,
    val maxSumeniResource: Float = 4f,

    // Inspiration (0–6)
    val inspirationPoints: Int = 0,

    // Experience points
    val experiencePoints: Int = 0,

    // Wallet — amount of Crowns (Кроны)
    val crowns: Int = 0,

    val updatedAt: Long = System.currentTimeMillis()
) {
    // Backwards compatibility getters
    val wellbeing: Int get() = constitution
    val combat: Int get() = willpower
    val thinking: Int get() = intelligence
    val attention: Int get() = perception
    val communication: Int get() = influence
}
