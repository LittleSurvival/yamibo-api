package io.github.littlesurvival.render.util

/**
 * Utility for parsing CSS / HTML colour values into ARGB [Long].
 *
 * Supports:
 * - `#RGB`, `#RRGGBB`, `#AARRGGBB`
 * - `rgb(r, g, b)`, `rgba(r, g, b, a)`
 * - Named colours (a common subset used on the Yamibo forum)
 *
 * Returns `null` for any unrecognised value — the renderer should fall back to its default colour.
 */
object ColorParser {

    /** Named colour map (lowercase key → ARGB). */
    private val NAMED_COLORS: Map<String, Long> =
            mapOf(
                    "black" to 0xFF000000,
                    "white" to 0xFFFFFFFF,
                    "red" to 0xFFFF0000,
                    "green" to 0xFF008000,
                    "blue" to 0xFF0000FF,
                    "yellow" to 0xFFFFFF00,
                    "orange" to 0xFFFFA500,
                    "purple" to 0xFF800080,
                    "pink" to 0xFFFFC0CB,
                    "gray" to 0xFF808080,
                    "grey" to 0xFF808080,
                    "brown" to 0xFFA52A2A,
                    "cyan" to 0xFF00FFFF,
                    "magenta" to 0xFFFF00FF,
                    "lime" to 0xFF00FF00,
                    "navy" to 0xFF000080,
                    "teal" to 0xFF008080,
                    "olive" to 0xFF808000,
                    "maroon" to 0xFF800000,
                    "silver" to 0xFFC0C0C0,
                    "darkred" to 0xFF8B0000,
                    "darkgreen" to 0xFF006400,
                    "darkblue" to 0xFF00008B,
                    "sienna" to 0xFFA0522D,
                    "saddlebrown" to 0xFF8B4513,
                    "darkorange" to 0xFFFF8C00,
                    "indigo" to 0xFF4B0082,
                    "violet" to 0xFFEE82EE,
                    "coral" to 0xFFFF7F50,
                    "tomato" to 0xFFFF6347,
                    "gold" to 0xFFFFD700,
                    "khaki" to 0xFFF0E68C,
                    "crimson" to 0xFFDC143C,
                    "chocolate" to 0xFFD2691E,
            )

    /**
     * Parse a colour string to an ARGB [Long].
     *
     * @return ARGB value, or `null` if parsing fails.
     */
    fun parse(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()

        // Named colour
        NAMED_COLORS[trimmed.lowercase()]?.let {
            return it
        }

        // Hex (#RGB / #RRGGBB / #AARRGGBB)
        if (trimmed.startsWith('#')) {
            return parseHex(trimmed.substring(1))
        }

        // rgb(r, g, b) or rgba(r, g, b, a)
        if (trimmed.startsWith("rgb", ignoreCase = true)) {
            return parseRgbFunction(trimmed)
        }

        return null
    }

    private fun parseHex(hex: String): Long? {
        return when (hex.length) {
            3 -> {
                // #RGB → #RRGGBB
                val r = hex[0].digitToIntOrNull(16) ?: return null
                val g = hex[1].digitToIntOrNull(16) ?: return null
                val b = hex[2].digitToIntOrNull(16) ?: return null
                (0xFF000000) or
                        ((r * 17).toLong() shl 16) or
                        ((g * 17).toLong() shl 8) or
                        (b * 17).toLong()
            }
            6 -> {
                val value = hex.toLongOrNull(16) ?: return null
                0xFF000000 or value
            }
            8 -> {
                hex.toLongOrNull(16)
            }
            else -> null
        }
    }

    private fun parseRgbFunction(raw: String): Long? {
        // Extract content between parentheses
        val start = raw.indexOf('(')
        val end = raw.lastIndexOf(')')
        if (start < 0 || end <= start) return null

        val parts = raw.substring(start + 1, end).split(',').map { it.trim() }

        if (parts.size < 3) return null

        val r = parts[0].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val g = parts[1].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val b = parts[2].toIntOrNull()?.coerceIn(0, 255) ?: return null

        val a =
                if (parts.size >= 4) {
                    val af = parts[3].toFloatOrNull() ?: return null
                    (af.coerceIn(0f, 1f) * 255).toInt()
                } else {
                    255
                }

        return (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}
