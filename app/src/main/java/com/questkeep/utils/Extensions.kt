package com.questkeep.utils

import kotlin.random.Random

fun generateInviteCode(length: Int = 6): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..length).map { chars.random() }.joinToString("")
}

fun Float.toResourceDisplay(): String {
    return if (this == this.toInt().toFloat()) {
        this.toInt().toString()
    } else {
        String.format("%.1f", this)
    }
}

fun Int.hpColor(): androidx.compose.ui.graphics.Color {
    return when {
        this <= 0 -> com.questkeep.ui.theme.AccentRed
        this < 30 -> com.questkeep.ui.theme.AccentOrange
        else -> com.questkeep.ui.theme.AccentGreen
    }
}
