package com.questkeep.domain.entities

data class SharedInventoryItem(
    val id: String = "",
    val campaignId: String = "",
    val name: String = "",
    val description: String = "",
    val quantity: Int = 1,
    val depositedBy: String = "",  // userId who put it in the bank
    val createdAt: Long = System.currentTimeMillis()
)
