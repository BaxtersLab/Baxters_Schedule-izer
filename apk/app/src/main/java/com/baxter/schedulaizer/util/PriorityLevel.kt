package com.baxter.schedulaizer.util

enum class PriorityLevel(val value: Int, val colorHex: String, val displayName: String) {
    CRITICAL(0, "#D32F2F", "Critical"),
    HIGH(1, "#F57C00", "High"),
    NORMAL(2, "#1565C0", "Normal"),
    LOW(3, "#757575", "Low");

    companion object {
        fun fromInt(value: Int): PriorityLevel = when (value) {
            0 -> CRITICAL
            1 -> HIGH
            2 -> NORMAL
            3 -> LOW
            else -> NORMAL
        }
    }
}
