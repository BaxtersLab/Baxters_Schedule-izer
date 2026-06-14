package com.baxter.schedulaizer.util

enum class EventCategory(val displayName: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    BILLS("Bills"),
    MEDICAL("Medical"),
    OTHER("Other");

    companion object {
        fun fromString(name: String): EventCategory {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: OTHER
        }
    }
}
