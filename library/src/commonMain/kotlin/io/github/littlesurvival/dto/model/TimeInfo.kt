package io.github.littlesurvival.dto.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@Serializable
data class TimeInfo(
    val text: String,
    val specialText: String? = null,
    val epoch: Long,
) {
    companion object {
        fun parse(rawText: String): TimeInfo {
            val dateMatch = Regex("""\d{4}-\d{1,2}-\d{1,2}(?: \d{1,2}:\d{1,2}(?::\d{1,2})?)?""").find(rawText)
            val dateText = dateMatch?.value ?: rawText
            val specialText = if (rawText != dateText) rawText else null
            val epoch = textToEpoch(dateText)
            return TimeInfo(text = dateText, specialText = specialText, epoch = epoch)
        }

        fun textToEpoch(text: String): Long {
            val parts = text.split("-", " ", ":")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 1970
            val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
            val hour = parts.getOrNull(3)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val second = parts.getOrNull(5)?.toIntOrNull() ?: 0

            return try {
                val dateTime = LocalDateTime(year, month, day, hour, minute, second)
                // Yamibo uses UTC+8 (Beijing/Taipei Time)
                val instant = dateTime.toInstant(TimeZone.of("UTC+08:00"))
                instant.epochSeconds
            } catch (_: Exception) {
                0L
            }
        }
    }
}