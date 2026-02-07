package com.example.project.data.model

import java.time.DayOfWeek
import java.util.concurrent.TimeUnit

sealed class Frequency {
    abstract val interval: Int
    abstract fun toMillis(): Long
    abstract fun toDisplayString(): String
    abstract fun getTypeName(): String

    fun toStorageString(): String {
        val type = when (this) {
            is Hourly -> "HOURLY"
            is Daily -> "DAILY"
            is Weekly -> "WEEKLY"
            is Monthly -> "MONTHLY"
        }
        // Format: TYPE|INTERVAL|DAYS
        val days = daysOfWeek?.joinToString(",") { it.name } ?: "NONE"
        return "$type|$interval|$days"
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
        fun fromStorageString(value: String): Frequency {
            return try {
                val parts = value.split("|")
                val type = parts[0]
                val interval = parts.getOrNull(1)?.toInt() ?: 1
                val daysStr = parts.getOrNull(2)

                val days = if (daysStr == null || daysStr == "NONE") null
                else daysStr.split(",").map { DayOfWeek.valueOf(it) }.toSet()

                when (type) {
                    "HOURLY" -> Hourly(interval)
                    "DAILY" -> Daily(interval, days)
                    "WEEKLY" -> Weekly(interval, days)
                    "MONTHLY" -> Monthly(interval)
                    else -> Daily(1)
                }
            } catch (e: Exception) {
                Daily(1) // Fallback on error
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