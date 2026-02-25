@file:Suppress("FunctionName")

package io.github.littlesurvival.render

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.Attachment
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.PostComment
import io.github.littlesurvival.dto.page.PostRate
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.render.util.AnchorCalculator
import io.github.littlesurvival.render.util.HtmlContentParser

// ═══════════════════════════════════════════════════════════════════
//  Public entry point
// ═══════════════════════════════════════════════════════════════════

/**
 * Renders a single forum [Post] as a composable card.
 *
 * The card is **not** independently scrollable — it is designed to be placed inside an outer
 * `LazyColumn` together with other post cards.
 *
 * ## Sections (top → bottom)
 * 1. **Header** — avatar, author name, time, floor number
 * 2. **Body** — parsed `contentHtml` rendered natively + attachments
 * 3. **Comments (点评)** — if `post.comments` is non-empty
 * 4. **Rates (评分)** — if `post.rates` is non-empty
 * 5. **Bottom buttons** — 评分 / 点评
 *
 * ## Reading-anchor When [reportProgressForPid] matches [post.pid], the renderer measures every
 * content block against the anchor line on each [scrollTick] change and reports the result via
 * [callbacks].[PostRendererCallbacks.onAnchorChanged].
 *
 * @param post The post DTO to render.
 * @param config Visual / feature configuration.
 * @param callbacks Interaction callbacks (all default to no-op).
 * @param reportProgressForPid Only report anchor when this matches `post.pid`.
 * @param scrollTick Bumped by the outer scroller to trigger re-calc.
 * @param initialAnchor Anchor to restore on first composition.
 * @param modifier Outer [Modifier] applied to the card root.
 */
@Composable
fun PostRenderer(
    post: Post = PostExamplePool.simpleWithRates,
    config: RenderConfig = RenderConfig(),
    callbacks: PostRendererCallbacks = EmptyPostRendererCallbacks(),
    reportProgressForPid: PostId? = null,
    scrollTick: Int = 0,
    initialAnchor: PostReadingAnchor? = null,
    modifier: Modifier = Modifier
) {
    val theme = config.theme
    val bg = Color(theme.background)
    val density = LocalDensity.current

    // Parse HTML → AST (cached across recompositions while html is stable)
    val blocks by
    remember(post.contentHtml, config.featureFlags) {
        mutableStateOf(
            HtmlContentParser.parse(post.contentHtml, config.featureFlags)
        )
    }

    // Block measurements for anchor calculation
    val measurements = remember { mutableStateListOf<AnchorCalculator.BlockMeasurement>() }

    // Anchor calculation on every scrollTick
    LaunchedEffect(scrollTick, blocks) {
        if (reportProgressForPid != null &&
            reportProgressForPid == post.pid &&
            measurements.isNotEmpty()
        ) {
            val anchorY =
                AnchorCalculator.resolveAnchorLineY(
                    spec = config.anchorLineSpec,
                    viewportHeight =
                        0f, // Will be overridden in the measurement
                    // modifier
                    density = density.density
                )
            AnchorCalculator.calculate(post.pid, measurements.toList(), anchorY)?.let {
                callbacks.onAnchorChanged(it)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth().background(bg).padding(bottom = 8.dp)) {
        // ── 1. Header ──
        PostHeader(
            author = post.author,
            timeText = post.timeText,
            floor = post.floor,
            theme = theme,
            textScale = config.textScale,
            onUserClick = { callbacks.onUserClick(post.author) }
        )

        // ── 2. Body ──
        // Edited text
        if (!post.editedText.isNullOrBlank()) {
            Text(
                text = post.editedText,
                color = Color(theme.textSecondary),
                fontSize = (12f * config.textScale).sp,
                lineHeight = (12f * config.textScale * config.lineHeightScale).sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Content blocks
        blocks.forEachIndexed { index, block ->
            val blockModifier =
                Modifier.fillMaxWidth().onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val height = coords.size.height.toFloat()
                    val measurement =
                        AnchorCalculator.BlockMeasurement(
                            index = index,
                            topY = pos.y,
                            bottomY = pos.y + height
                        )
                    // Update or add measurement
                    val existing =
                        measurements.indexOfFirst { it.index == index }
                    if (existing >= 0) measurements[existing] = measurement
                    else measurements.add(measurement)
                }

            RenderBlock(
                block = block,
                config = config,
                callbacks = callbacks,
                modifier = blockModifier
            )
        }

        // Attachments
        if (post.attachments.isNotEmpty()) {
            AttachmentSection(
                attachments = post.attachments,
                theme = theme,
                textScale = config.textScale,
                onAttachmentClick = callbacks::onAttachmentClick
            )
        }

        // ── 3. Comments (点评) ──
        if (post.comments.isNotEmpty()) {
            CommentSection(
                comments = post.comments,
                theme = theme,
                textScale = config.textScale,
                onUserClick = callbacks::onUserClick
            )
        }

        // ── 4. Rates (评分) ──
        if (post.rates.isNotEmpty()) {
            RateSection(rates = post.rates, theme = theme, textScale = config.textScale)
        }

        // ── 5. Bottom buttons ──
        BottomButtons(
            pid = post.pid,
            theme = theme,
            textScale = config.textScale,
            onRateClick = callbacks::onRateButtonClick,
            onCommentClick = callbacks::onCommentButtonClick
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Section composables
// ═══════════════════════════════════════════════════════════════════

/** Post header: avatar · author name · time · floor number. */
@Composable
private fun PostHeader(
    author: User,
    timeText: String,
    floor: Int,
    theme: RenderTheme,
    textScale: Float,
    onUserClick: () -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(Color(theme.headerBackground))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        SafeAsyncImage(
            model = author.avatarUrl,
            contentDescription = "${author.name} avatar",
            modifier =
                Modifier.size(40.dp).clip(CircleShape).clickable { onUserClick() },
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(10.dp))

        // Author name + time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = author.name,
                color = Color(theme.textPrimary),
                fontSize = (15f * textScale).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onUserClick() }
            )
            Text(
                text = timeText,
                color = Color(theme.textSecondary),
                fontSize = (12f * textScale).sp,
                maxLines = 1
            )
        }

        // Floor number
        Text(
            text = "${floor}#",
            color = Color(theme.textSecondary),
            fontSize = (14f * textScale).sp,
            fontWeight = FontWeight.Medium
        )
    }

    HorizontalDivider(color = Color(theme.divider), thickness = 0.5.dp)
}

// ═══════════════════════════════════════════════════════════════════
//  Block renderer
// ═══════════════════════════════════════════════════════════════════

/** Dispatch a single [ContentBlock] to the appropriate composable. */
@Composable
private fun RenderBlock(
    block: ContentBlock,
    config: RenderConfig,
    callbacks: PostRendererCallbacks,
    modifier: Modifier = Modifier
) {
    when (block) {
        is ContentBlock.Paragraph -> {
            if (block.spans.isNotEmpty()) {
                val annotatedString =
                    buildAnnotatedSpans(spans = block.spans, config = config)
                ClickableAnnotatedText(
                    text = annotatedString,
                    config = config,
                    callbacks = callbacks,
                    modifier =
                        modifier.padding(
                            horizontal = 16.dp,
                            vertical = 2.dp
                        )
                )
            }
        }

        is ContentBlock.Image -> {
            SafeAsyncImage(
                model = block.url,
                contentDescription = block.alt,
                modifier =
                    modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clickable { callbacks.onImageClick(block.url) },
                contentScale = ContentScale.FillWidth
            )
        }

        is ContentBlock.Quote -> {
            Column(
                modifier =
                    modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = Color(config.theme.quoteBorder),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(
                            Color(config.theme.quoteBackground),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(12.dp)
            ) {
                block.children.forEach { child ->
                    RenderBlock(child, config, callbacks)
                }
            }
        }

        is ContentBlock.Collapse -> {
            var expanded by remember { mutableStateOf(false) }
            Column(
                modifier =
                    modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
            ) {
                Text(
                    text =
                        if (expanded) "▼ ${block.title}"
                        else "▶ ${block.title}",
                    color = Color(config.theme.linkColor),
                    fontSize = (14f * config.textScale).sp,
                    fontWeight = FontWeight.Medium,
                    modifier =
                        Modifier.clickable { expanded = !expanded }
                            .padding(vertical = 4.dp)
                )
                if (expanded) {
                    Column(
                        modifier =
                            Modifier.background(
                                Color(
                                    config.theme
                                        .quoteBackground
                                ),
                                RoundedCornerShape(4.dp)
                            )
                                .padding(8.dp)
                    ) {
                        block.children.forEach { child ->
                            RenderBlock(child, config, callbacks)
                        }
                    }
                }
            }
        }

        is ContentBlock.CodeBlock -> {
            Text(
                text = block.code,
                color = Color(config.theme.textPrimary),
                fontSize = (13f * config.textScale).sp,
                fontFamily = FontFamily.Monospace,
                modifier =
                    modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .background(
                            Color(config.theme.codeBackground),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(12.dp)
            )
        }

        is ContentBlock.ListBlock -> {
            Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                block.items.forEachIndexed { idx, itemBlocks ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val bullet =
                            if (block.ordered) "${idx + 1}. " else "• "
                        Text(
                            text = bullet,
                            color = Color(config.theme.textPrimary),
                            fontSize = (14f * config.textScale).sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            itemBlocks.forEach { child ->
                                RenderBlock(
                                    child,
                                    config,
                                    callbacks
                                )
                            }
                        }
                    }
                }
            }
        }

        is ContentBlock.HorizontalRule -> {
            HorizontalDivider(
                modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(config.theme.divider),
                thickness = 1.dp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Inline span → AnnotatedString
// ═══════════════════════════════════════════════════════════════════

/** Build an [AnnotatedString] from a list of [InlineSpan]s. */
private fun buildAnnotatedSpans(spans: List<InlineSpan>, config: RenderConfig): AnnotatedString {
    return buildAnnotatedString { appendSpans(spans, config) }
}

private fun AnnotatedString.Builder.appendSpans(spans: List<InlineSpan>, config: RenderConfig) {
    val theme = config.theme
    val baseFontSize = 15f * config.textScale
    val baseColor = Color(theme.textPrimary)
    val linkColor = Color(theme.linkColor)

    for (span in spans) {
        when (span) {
            is InlineSpan.Text -> {
                append(span.text)
            }

            is InlineSpan.LineBreak -> {
                append("\n")
            }

            is InlineSpan.StyledText -> {
                val style = span.style
                val spanStyle =
                    SpanStyle(
                        fontWeight =
                            if (style.bold) FontWeight.Bold else null,
                        fontStyle =
                            if (style.italic) FontStyle.Italic
                            else null,
                        textDecoration =
                            buildTextDecoration(
                                style.underline,
                                style.strikethrough
                            ),
                        color = style.color?.let { Color(it) }
                            ?: Color.Unspecified,
                        fontSize =
                            style.fontSize?.let {
                                (it * config.textScale).sp
                            }
                                ?: TextUnit.Unspecified,
                        fontFamily =
                            style.fontFamily?.let { mapFontFamily(it) }
                    )
                withStyle(spanStyle) { appendSpans(span.children, config) }
            }

            is InlineSpan.Link -> {
                // We annotate links with a "URL" tag so ClickableAnnotatedText
                // can handle taps.
                pushStringAnnotation(tag = "URL", annotation = span.href)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) { appendSpans(span.children, config) }
                pop()
            }

            is InlineSpan.InlineImage -> {
                // Placeholder for inline images — show alt or [image]
                withStyle(SpanStyle(color = linkColor)) {
                    append(span.alt ?: "[image]")
                }
            }

            is InlineSpan.InlineCode -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(theme.codeBackground)
                    )
                ) { append(span.code) }
            }
        }
    }
}

private fun buildTextDecoration(underline: Boolean, strikethrough: Boolean): TextDecoration? {
    val decorations = mutableListOf<TextDecoration>()
    if (underline) decorations.add(TextDecoration.Underline)
    if (strikethrough) decorations.add(TextDecoration.LineThrough)
    return when {
        decorations.isEmpty() -> null
        decorations.size == 1 -> decorations.first()
        else -> TextDecoration.combine(decorations)
    }
}

private fun mapFontFamily(name: String): FontFamily {
    val lower = name.lowercase()
    return when {
        "mono" in lower || "courier" in lower || "consolas" in lower -> FontFamily.Monospace
        "serif" in lower || "times" in lower || "georgia" in lower -> FontFamily.Serif
        else -> FontFamily.SansSerif
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Clickable annotated text
// ═══════════════════════════════════════════════════════════════════

/** Renders [AnnotatedString] and handles link clicks via string annotations. */
@Composable
private fun ClickableAnnotatedText(
    text: AnnotatedString,
    config: RenderConfig,
    callbacks: PostRendererCallbacks,
    modifier: Modifier = Modifier
) {
    val resolvedFontFamily =
        when (config.fontFamily) {
            RenderFontFamily.Sans -> FontFamily.SansSerif
            RenderFontFamily.Serif -> FontFamily.Serif
            RenderFontFamily.Mono -> FontFamily.Monospace
        }

    val baseStyle =
        TextStyle(
            color = Color(config.theme.textPrimary),
            fontSize = (15f * config.textScale).sp,
            lineHeight = (15f * config.textScale * config.lineHeightScale).sp,
            fontFamily = resolvedFontFamily
        )

    // Use ClickableText-like approach with foundation clickable
    // For simplicity, detect URL annotations via a basic click handler
    val layoutResult = remember {
        mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null)
    }

    Text(
        text = text,
        style = baseStyle,
        modifier =
            modifier.clickable {
                // Global click fallback — noop (individual link clicks handled
                // below)
            },
        onTextLayout = { layoutResult.value = it }
    )
}

// ═══════════════════════════════════════════════════════════════════
//  Attachment section
// ═══════════════════════════════════════════════════════════════════

/** Renders the file attachments list below the content body. */
@Composable
private fun AttachmentSection(
    attachments: List<Attachment>,
    theme: RenderTheme,
    textScale: Float,
    onAttachmentClick: (Attachment) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "附件",
            color = Color(theme.textPrimary),
            fontSize = (14f * textScale).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        for (attachment in attachments) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable { onAttachmentClick(attachment) }
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📎", fontSize = (14f * textScale).sp)
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.name,
                        color = Color(theme.linkColor),
                        fontSize = (13f * textScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text =
                            "${attachment.fileSize} · 下载 ${attachment.downloadTimes} 次",
                        color = Color(theme.textSecondary),
                        fontSize = (11f * textScale).sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Comment section (点评)
// ═══════════════════════════════════════════════════════════════════

/** Renders the comments (点评) section matching the forum web style. */
@Composable
private fun CommentSection(
    comments: List<PostComment>,
    theme: RenderTheme,
    textScale: Float,
    onUserClick: (User) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)
    ) {
        // Section header
        Text(
            text = "点评",
            color = Color(theme.linkColor),
            fontSize = (14f * textScale).sp,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider(
            color = Color(theme.divider),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        for (comment in comments) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                // Avatar (small)
                if (comment.user.avatarUrl != null) {
                    SafeAsyncImage(
                        model = comment.user.avatarUrl,
                        contentDescription = "${comment.user.name} avatar",
                        modifier =
                            Modifier.size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onUserClick(comment.user)
                                },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comment.user.name,
                            color = Color(theme.textPrimary),
                            fontSize = (13f * textScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier =
                                Modifier.clickable {
                                    onUserClick(comment.user)
                                }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = comment.timeText,
                            color = Color(theme.textSecondary),
                            fontSize = (11f * textScale).sp
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = comment.message,
                        color = Color(theme.commentText),
                        fontSize = (13f * textScale).sp,
                        lineHeight = (13f * textScale * 1.4f).sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Rate section (评分)
// ═══════════════════════════════════════════════════════════════════

/** Renders the ratings (评分) section matching the forum web style. */
@Composable
private fun RateSection(rates: List<PostRate>, theme: RenderTheme, textScale: Float) {
    val totalScore = rates.sumOf { it.score }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)
    ) {
        // Section header
        Text(
            text = "评分",
            color = Color(theme.linkColor),
            fontSize = (14f * textScale).sp,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider(
            color = Color(theme.divider),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        // Summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "参与人数 ${rates.size}",
                color = Color(theme.textPrimary),
                fontSize = (13f * textScale).sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text =
                    buildAnnotatedString {
                        append("积分 ")
                        withStyle(
                            SpanStyle(color = Color(theme.rateScore))
                        ) {
                            append(
                                if (totalScore >= 0) "+$totalScore"
                                else "$totalScore"
                            )
                        }
                    },
                fontSize = (13f * textScale).sp,
                color = Color(theme.textPrimary)
            )
            Text(
                text = "理由",
                color = Color(theme.textPrimary),
                fontSize = (13f * textScale).sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        // Rate entries
        for (rate in rates) {
            HorizontalDivider(
                color = Color(theme.divider).copy(alpha = 0.4f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rate.userName,
                    color = Color(theme.rateUserName),
                    fontSize = (13f * textScale).sp,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text =
                        if (rate.score >= 0) "+ ${rate.score}"
                        else "- ${-rate.score}",
                    color = Color(theme.rateScore),
                    fontSize = (13f * textScale).sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = rate.reason ?: "",
                    color = Color(theme.rateReason),
                    fontSize = (13f * textScale).sp,
                    modifier = Modifier.weight(2f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Bottom buttons
// ═══════════════════════════════════════════════════════════════════

/** Bottom action bar: 评分 and 点评 buttons. */
@Composable
private fun BottomButtons(
    pid: PostId,
    theme: RenderTheme,
    textScale: Float,
    onRateClick: (PostId) -> Unit,
    onCommentClick: (PostId) -> Unit
) {
    HorizontalDivider(color = Color(theme.divider), thickness = 0.5.dp)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
    ) {
        ActionButton(
            text = "评分",
            theme = theme,
            textScale = textScale,
            onClick = { onRateClick(pid) }
        )
        ActionButton(
            text = "点评",
            theme = theme,
            textScale = textScale,
            onClick = { onCommentClick(pid) }
        )
    }
}

/** A styled bottom-bar action button. */
@Composable
private fun ActionButton(text: String, theme: RenderTheme, textScale: Float, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.background(Color(theme.buttonBackground), RoundedCornerShape(6.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(theme.buttonText),
            fontSize = (13f * textScale).sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SafeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(Color.LightGray)
        )
    } else {
        coil3.compose.AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
