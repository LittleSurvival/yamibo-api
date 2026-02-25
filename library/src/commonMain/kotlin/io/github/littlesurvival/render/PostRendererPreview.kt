@file:Suppress("FunctionName")

package io.github.littlesurvival.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

// ═══════════════════════════════════════════════════════════════════
//  Single-post previews
// ═══════════════════════════════════════════════════════════════════

/** Default preview — simple post with ratings (default theme). */
@Preview
@Composable
fun PreviewPostRenderer_SimpleWithRates() {
    val config = RenderConfig()
    PostRenderer(post = PostExamplePool.simpleWithRates, config = config)
}

/** Rich formatting post — bold, italic, colored text, links, and ratings. */
@Preview
@Composable
fun PreviewPostRenderer_RichFormatting() {
    val config = RenderConfig()
    PostRenderer(post = PostExamplePool.richFormatting, config = config)
}

/** Post with comments (点评) and ratings (评分). */
@Preview
@Composable
fun PreviewPostRenderer_WithComments() {
    val config = RenderConfig()
    PostRenderer(post = PostExamplePool.withComments, config = config)
}

/** Short reply post. */
@Preview
@Composable
fun PreviewPostRenderer_ReplyWithQuote() {
    val config = RenderConfig()
    PostRenderer(post = PostExamplePool.replyWithQuote, config = config)
}

/** Post with file attachments. */
@Preview
@Composable
fun PreviewPostRenderer_WithAttachments() {
    val config = RenderConfig()
    PostRenderer(post = PostExamplePool.withAttachments, config = config)
}

/** Minimal plain-text post — no formatting, no rates, no comments. */
@Preview
@Composable
fun PreviewPostRenderer_Minimal() {
    val config = RenderConfig()
    PostRenderer(post = PostExamplePool.minimal, config = config)
}

// ═══════════════════════════════════════════════════════════════════
//  Dark theme previews
// ═══════════════════════════════════════════════════════════════════

/** Dark theme — simple post with ratings. */
@Preview
@Composable
fun PreviewPostRenderer_DarkTheme() {
    val config = RenderConfig(theme = RenderTheme.Dark)
    PostRenderer(post = PostExamplePool.simpleWithRates, config = config)
}

/** Dark theme — post with comments. */
@Preview
@Composable
fun PreviewPostRenderer_DarkTheme_WithComments() {
    val config = RenderConfig(theme = RenderTheme.Dark)
    PostRenderer(post = PostExamplePool.withComments, config = config)
}

// ═══════════════════════════════════════════════════════════════════
//  Scale / config previews
// ═══════════════════════════════════════════════════════════════════

/** Large text scale preview (1.4×). */
@Preview
@Composable
fun PreviewPostRenderer_LargeText() {
    val config = RenderConfig(textScale = 1.4f)
    PostRenderer(post = PostExamplePool.richFormatting, config = config)
}

/** Small text scale preview (0.8×). */
@Preview
@Composable
fun PreviewPostRenderer_SmallText() {
    val config = RenderConfig(textScale = 0.8f)
    PostRenderer(post = PostExamplePool.richFormatting, config = config)
}

// ═══════════════════════════════════════════════════════════════════
//  Multi-post thread preview (simulates LazyColumn)
// ═══════════════════════════════════════════════════════════════════

/**
 * Thread view — all sample posts stacked (like a real thread page).
 *
 * Scroll to see posts beyond the viewport.
 */
@Preview
@Composable
fun PreviewPostRenderer_ThreadView() {
    val config = RenderConfig()
    val bg = Color(config.theme.background)

    Column(modifier = Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState())) {
        for (post in PostExamplePool.all) {
            PostRenderer(post = post, config = config, modifier = Modifier.fillMaxWidth())
            HorizontalDivider(color = Color(config.theme.divider), thickness = 4.dp)
        }
    }
}

/** Thread view — dark theme variant. */
@Preview
@Composable
fun PreviewPostRenderer_ThreadView_Dark() {
    val config = RenderConfig(theme = RenderTheme.Dark)
    val bg = Color(config.theme.background)

    Column(modifier = Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState())) {
        for (post in PostExamplePool.all) {
            PostRenderer(post = post, config = config, modifier = Modifier.fillMaxWidth())
            HorizontalDivider(color = Color(config.theme.divider), thickness = 4.dp)
        }
    }
}
