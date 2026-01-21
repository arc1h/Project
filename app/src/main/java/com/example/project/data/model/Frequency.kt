package com.example.project.data.model

/**
 * Represents the frequency of a habit with type-safe intervals
 */
sealed class Frequency {
    abstract val interval: Int

    data class Hourly(override val interval: Int = 1) : Frequency()
    data class Daily(override val interval: Int = 1) : Frequency()
    data class Weekly(override val interval: Int = 1) : Frequency()
    data class Monthly(override val interval: Int = 1) : Frequency()
    data class Yearly(override val interval: Int = 1) : Frequency()

    /**
     * Convert frequency to milliseconds
     */
    fun toMillis(): Long = when (this) {
        is Hourly -> interval * 60L * 60L * 1000L
        is Daily -> interval * 24L * 60L * 60L * 1000L
        is Weekly -> interval * 7L * 24L * 60L * 60L * 1000L
        is Monthly -> interval * 30L * 24L * 60L * 60L * 1000L
        is Yearly -> interval * 365L * 24L * 60L * 60L * 1000L
    }

    /**
     * Convert to human-readable string for display
     */
    fun toDisplayString(): String = when (this) {
        is Hourly -> if (interval == 1) "Hourly" else "Every $interval Hours"
        is Daily -> if (interval == 1) "Daily" else "Every $interval Days"
        is Weekly -> if (interval == 1) "Weekly" else "Every $interval Weeks"
        is Monthly -> if (interval == 1) "Monthly" else "Every $interval Months"
        is Yearly -> if (interval == 1) "Yearly" else "Every $interval Years"
    }

    /**
     * Convert to storage format for Firestore
     */
    fun toStorageString(): String = when (this) {
        is Hourly -> "Hourly (Every $interval ${if (interval == 1) "Hour" else "Hours"})"
        is Daily -> "Daily (Every $interval ${if (interval == 1) "Day" else "Days"})"
        is Weekly -> "Weekly (Every $interval ${if (interval == 1) "Week" else "Weeks"})"
        is Monthly -> "Monthly (Every $interval ${if (interval == 1) "Month" else "Months"})"
        is Yearly -> "Yearly (Every $interval ${if (interval == 1) "Year" else "Years"})"
    }

    /**
     * Get the type name (for dropdown selection)
     */
    fun getTypeName(): String = when (this) {
        is Hourly -> "hourly"
        is Daily -> "daily"
        is Weekly -> "weekly"
        is Monthly -> "monthly"
        is Yearly -> "yearly"
    }

    companion object {
        /**
         * Parse from storage string format
         */
        fun fromStorageString(str: String): Frequency {
            val regex = """(\w+)\s*\(every\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(str) ?: return Daily(1)

            val type = match.groupValues[1].lowercase()
            val interval = match.groupValues[2].toIntOrNull()?.coerceIn(1, 999) ?: 1

            return when (type) {
                "hourly" -> Hourly(interval)
                "daily" -> Daily(interval)
                "weekly" -> Weekly(interval)
                "monthly" -> Monthly(interval)
                "yearly" -> Yearly(interval)
                else -> Daily(1)
            }
        }

        /**
         * Create from type and interval
         */
        fun fromTypeAndInterval(type: String, interval: Int): Frequency {
            val safeInterval = interval.coerceIn(1, 999)
            return when (type.lowercase()) {
                "hourly" -> Hourly(safeInterval)
                "daily" -> Daily(safeInterval)
                "weekly" -> Weekly(safeInterval)
                "monthly" -> Monthly(safeInterval)
                "yearly" -> Yearly(safeInterval)
                else -> Daily(safeInterval)
            }
        }

        /**
         * Get unit name for interval display
         */
        fun getUnitName(type: String, count: Int): String {
            return when (type.lowercase()) {
                "hourly" -> if (count == 1) "Hour" else "Hours"
                "daily" -> if (count == 1) "Day" else "Days"
                "weekly" -> if (count == 1) "Week" else "Weeks"
                "monthly" -> if (count == 1) "Month" else "Months"
                "yearly" -> if (count == 1) "Year" else "Years"
                else -> if (count == 1) "Day" else "Days"
            }
        }
    }
}