package io.github.littlesurvival.render.util

import io.github.littlesurvival.render.FeatureFlags
import io.github.littlesurvival.render.StyleBundle

/**
 * Extracts a [StyleBundle] from an HTML element's attributes and inline `style` property.
 *
 * Controlled by [FeatureFlags] — when a flag is off the corresponding style property is dropped
 * (left as `null`/`false`).
 */
object StyleExtractor {

    /**
     * Build a [StyleBundle] from an element's tag name, attributes, and inline style string.
     *
     * @param tagName Lowercase tag name (e.g. "b", "font", "span").
     * @param attrs Map of attribute name → value.
     * @param inlineStyle Raw value of the `style` attribute (nullable).
     * @param flags Current feature flags.
     */
    fun extract(
            tagName: String,
            attrs: Map<String, String>,
            inlineStyle: String?,
            flags: FeatureFlags
    ): StyleBundle {
        var bold = false
        var italic = false
        var underline = false
        var strikethrough = false
        var color: Long? = null
        var fontSize: Float? = null
        var fontFamily: String? = null

        // ── Tag-level semantics ──
        when (tagName) {
            "b", "strong" -> bold = true
            "i", "em" -> italic = true
            "u" -> underline = true
            "s", "del", "strike" -> strikethrough = true
        }

        // ── <font> attributes ──
        if (tagName == "font") {
            if (flags.enablePostColors) {
                color = ColorParser.parse(attrs["color"])
            }
            if (flags.enablePostFontSizes) {
                fontSize = parseFontSizeAttr(attrs["size"])
            }
            if (flags.enablePostFonts) {
                fontFamily = attrs["face"]?.takeIf { it.isNotBlank() }
            }
        }

        // ── Inline style ──
        if (!inlineStyle.isNullOrBlank()) {
            parseInlineStyles(inlineStyle).forEach { (prop, value) ->
                when (prop) {
                    "color" ->
                            if (flags.enablePostColors) {
                                ColorParser.parse(value)?.let { color = it }
                            }
                    "font-size" ->
                            if (flags.enablePostFontSizes) {
                                parseCssFontSize(value)?.let { fontSize = it }
                            }
                    "font-family" ->
                            if (flags.enablePostFonts) {
                                fontFamily =
                                        value.trim().removeSurrounding("'").removeSurrounding("\"")
                            }
                    "font-weight" -> {
                        if (value.trim().let {
                                    it == "bold" || it == "700" || it == "800" || it == "900"
                                }
                        ) {
                            bold = true
                        }
                    }
                    "font-style" -> {
                        if (value.trim() == "italic") italic = true
                    }
                    "text-decoration" -> {
                        val v = value.trim()
                        if ("underline" in v) underline = true
                        if ("line-through" in v) strikethrough = true
                    }
                }
            }
        }

        return StyleBundle(
                bold = bold,
                italic = italic,
                underline = underline,
                strikethrough = strikethrough,
                color = color,
                fontSize = fontSize,
                fontFamily = fontFamily
        )
    }

    // ── Helpers ──

    /** Parse a `style="..."` string into key-value pairs. */
    private fun parseInlineStyles(style: String): List<Pair<String, String>> {
        return style.split(';').mapNotNull { part ->
            val colon = part.indexOf(':')
            if (colon < 0) return@mapNotNull null
            val key = part.substring(0, colon).trim().lowercase()
            val value = part.substring(colon + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) key to value else null
        }
    }

    /**
     * HTML `<font size="N">` → sp value.
     *
     * HTML font sizes 1–7 mapped to approximate sp equivalents.
     */
    private fun parseFontSizeAttr(size: String?): Float? {
        if (size.isNullOrBlank()) return null
        return when (size.trim()) {
            "1" -> 10f
            "2" -> 13f
            "3" -> 16f // default
            "4" -> 18f
            "5" -> 24f
            "6" -> 32f
            "7" -> 48f
            else -> size.trim().toFloatOrNull()
        }
    }

    /** CSS `font-size: Xpx | Xpt | Xem | X%` → sp value. */
    private fun parseCssFontSize(value: String): Float? {
        val v = value.trim().lowercase()
        return when {
            v.endsWith("px") -> v.removeSuffix("px").trim().toFloatOrNull()
            v.endsWith("pt") -> v.removeSuffix("pt").trim().toFloatOrNull()?.times(1.333f)
            v.endsWith("em") -> v.removeSuffix("em").trim().toFloatOrNull()?.times(16f)
            v.endsWith("rem") -> v.removeSuffix("rem").trim().toFloatOrNull()?.times(16f)
            v.endsWith("%") -> v.removeSuffix("%").trim().toFloatOrNull()?.div(100f)?.times(16f)
            else -> v.toFloatOrNull()
        }
    }
}
