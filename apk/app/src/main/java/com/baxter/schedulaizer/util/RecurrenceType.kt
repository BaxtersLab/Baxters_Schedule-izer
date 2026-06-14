package com.baxter.schedulaizer.util

enum class RecurrenceType(val displayName: String) {
    NONE("One-time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly");

    companion object {
        fun fromString(name: String): RecurrenceType {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
        }
    }
}
