package com.example.project.data.model

import java.time.DayOfWeek
import java.util.concurrent.TimeUnit

sealed class Frequency {
    abstract val interval: Int
    abstract fun toMillis(): Long
    abstract fun toDisplayString(): String
    abstract fun getTypeName(): String

    // FIX 1: Move this OUT of the sealed class body or
    // simply make it a regular abstract function/normal function
    fun toStorageString(): String {
        return when (this) {
            is Hourly -> "HOURLY"
            is Daily -> "DAILY"
            is Weekly -> "WEEKLY"
            is Monthly -> "MONTHLY"
        }
    }

    open val daysOfWeek: Set<DayOfWeek>? = null

    data class Hourly(override val interval: Int = 1) : Frequency() {
        override fun toMillis() = TimeUnit.HOURS.toMillis(interval.toLong())
        override fun toDisplayString() = if (interval == 1) "Every hour" else "Every $interval hours"
        override fun getTypeName() = "hourly"
    }

    data class Daily(
        override val interval: Int = 1,
        override val daysOfWeek: Set<DayOfWeek>? = null
    ) : Frequency() {
        override fun toMillis() = TimeUnit.DAYS.toMillis(interval.toLong())
        override fun toDisplayString(): String {
            return when {
                daysOfWeek == null -> if (interval == 1) "Every day" else "Every $interval days"
                daysOfWeek.size == 7 -> "Every day"
                // ... rest of your logic ...
                else -> daysOfWeek.sortedBy { it.value }.joinToString(", ") { it.name.take(3) }
            }
        }
        override fun getTypeName() = "daily"
    }

    data class Weekly(
        override val interval: Int = 1,
        override val daysOfWeek: Set<DayOfWeek>? = null
    ) : Frequency() {
        override fun toMillis() = TimeUnit.DAYS.toMillis(7L * interval)
        override fun toDisplayString() = if (interval == 1) "Every week" else "Every $interval weeks"
        override fun getTypeName() = "weekly"
    }

    data class Monthly(override val interval: Int = 1) : Frequency() {
        override fun toMillis() = TimeUnit.DAYS.toMillis(30L * interval)
        override fun toDisplayString() = if (interval == 1) "Every month" else "Every $interval months"
        override fun getTypeName() = "monthly"
    }

    companion object {
        // FIX 2: Add this so Frequency.fromStorageString(val) works!
        fun fromStorageString(value: String): Frequency {
            return when (value.uppercase()) {
                "HOURLY" -> Hourly()
                "DAILY" -> Daily()
                "WEEKLY" -> Weekly()
                "MONTHLY" -> Monthly()
                else -> Daily() // Safe fallback
            }
        }

        fun fromTypeAndInterval(
            type: String,
            interval: Int,
            daysOfWeek: Set<DayOfWeek>? = null
        ): Frequency {
            return when (type.lowercase()) {
                "hourly" -> Hourly(interval)
                "daily" -> Daily(interval, daysOfWeek)
                "weekly" -> Weekly(interval, daysOfWeek)
                "monthly" -> Monthly(interval)
                else -> Daily(1)
            }
        }

        fun getUnitName(type: String, count: Int): String {
            return when (type.lowercase()) {
                "hourly" -> if (count == 1) "hour" else "hours"
                "daily" -> if (count == 1) "day" else "days"
                "weekly" -> if (count == 1) "week" else "weeks"
                "monthly" -> if (count == 1) "month" else "months"
                else -> "day"
            }
        }
    }
}