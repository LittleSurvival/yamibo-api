package io.github.littlesurvival.render

/**
 * Configuration for the PostRenderer.
 *
 * Controls visual appearance, feature toggles, and reading-anchor behaviour.
 * App layer creates and passes this config; the renderer recomposes when any
 * value changes.
 *
 * @property textScale       Global text scaling factor (1.0 = 100%).
 * @property lineHeightScale Line-height multiplier relative to font size.
 * @property theme           Color theme used by the renderer.
 * @property fontFamily      Desired font family category.
 * @property featureFlags    Toggles for optional rendering features.
 * @property anchorLineSpec  Specification of the anchor line used for
 *                           reading-progress calculation.
 * @property configVersion   Monotonically increasing version; bump when the
 *                           app-level settings change so that persisted
 *                           anchors can be invalidated if needed.
 */
data class RenderConfig(
    val textScale: Float = 1.0f,
    val lineHeightScale: Float = 1.4f,
    val theme: RenderTheme = RenderTheme.Default,
    val fontFamily: RenderFontFamily = RenderFontFamily.Sans,
    val featureFlags: FeatureFlags = FeatureFlags(),
    val anchorLineSpec: AnchorLineSpec = AnchorLineSpec(),
    val configVersion: Int = 1
)

/**
 * Color theme for the post renderer.
 *
 * Each field is an ARGB hex color expressed as a [Long] (0xFFRRGGBB).
 *
 * @property background           Card / page background.
 * @property textPrimary          Default body-text color (no inline style).
 * @property textSecondary        Time / edit metadata text color.
 * @property rateUserName         User-name color in rating section.
 * @property rateReason           Reason text color in rating section.
 * @property rateScore            Score value color in rating section.
 * @property commentText          Comment message text color.
 * @property divider              Separator / border color.
 * @property linkColor            Hyperlink text color.
 * @property quoteBackground      Background for <blockquote> / div.quote.
 * @property quoteBorder          Left-border accent of quotes.
 * @property codeBackground       Background for <code>/<pre> blocks.
 * @property headerBackground     Post header strip background.
 * @property buttonBackground     Bottom-bar button background.
 * @property buttonText           Bottom-bar button text color.
 */
data class RenderTheme(
    val background: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val rateUserName: Long,
    val rateReason: Long,
    val rateScore: Long,
    val commentText: Long,
    val divider: Long,
    val linkColor: Long,
    val quoteBackground: Long,
    val quoteBorder: Long,
    val codeBackground: Long,
    val headerBackground: Long,
    val buttonBackground: Long,
    val buttonText: Long
) {
    companion object {
        /** Default theme matching the Yamibo web forum style. */
        val Default = RenderTheme(
            background       = 0xFFFCF4CF,
            textPrimary      = 0xFF6E2B19,
            textSecondary    = 0xFFA6A6A6,
            rateUserName     = 0xFF999999,
            rateReason       = 0xFF999999,
            rateScore        = 0xFFFF5656,
            commentText      = 0xFF333333,
            divider          = 0xFFE0D5B0,
            linkColor        = 0xFF8B4513,
            quoteBackground  = 0xFFF5EED5,
            quoteBorder      = 0xFFD4C494,
            codeBackground   = 0xFFF0E8C8,
            headerBackground = 0xFFFCF4CF,
            buttonBackground = 0xFFE8DDB8,
            buttonText       = 0xFF6E2B19
        )

        /** Dark theme variant. */
        val Dark = RenderTheme(
            background       = 0xFF1E1E1E,
            textPrimary      = 0xFFE0D0B0,
            textSecondary    = 0xFF808080,
            rateUserName     = 0xFF999999,
            rateReason       = 0xFF999999,
            rateScore        = 0xFFFF5656,
            commentText      = 0xFFCCCCCC,
            divider          = 0xFF3A3A3A,
            linkColor        = 0xFFD4A054,
            quoteBackground  = 0xFF2A2A2A,
            quoteBorder      = 0xFF555555,
            codeBackground   = 0xFF2A2A2A,
            headerBackground = 0xFF252525,
            buttonBackground = 0xFF333333,
            buttonText       = 0xFFE0D0B0
        )
    }
}

/** Font-family category used by the renderer. */
enum class RenderFontFamily {
    Sans,
    Serif,
    Mono
}

/**
 * Feature flags controlling optional HTML rendering behaviours.
 *
 * @property enablePostColors    Allow inline `color` styles from HTML.
 * @property enablePostFontSizes Allow inline `font-size` / `size` attrs.
 * @property enablePostFonts     Allow `font-family` / `face` attrs.
 * @property enableCollapse      Allow expand/collapse (showhide) blocks.
 */
data class FeatureFlags(
    val enablePostColors: Boolean = true,
    val enablePostFontSizes: Boolean = true,
    val enablePostFonts: Boolean = false,
    val enableCollapse: Boolean = true
)

/**
 * Specification of the anchor line for reading-progress calculation.
 *
 * The anchor line is a virtual horizontal line in the viewport.  Blocks that
 * straddle this line are considered the current reading position.
 *
 * @property offsetDp         Fixed offset from the top of the viewport (dp).
 *                            Used when [useRatio] is false.
 * @property ratio            Fraction of the viewport height (0.0–1.0).
 *                            Used when [useRatio] is true.
 * @property useRatio         Whether to use [ratio] instead of [offsetDp].
 */
data class AnchorLineSpec(
    val offsetDp: Float = 80f,
    val ratio: Float = 0.2f,
    val useRatio: Boolean = false
)
