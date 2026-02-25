package io.github.littlesurvival.render

/**
 * Abstract Syntax Tree (AST) for parsed HTML post content.
 *
 * The two-level hierarchy mirrors how rich text is typically structured:
 *
 * - **[ContentBlock]**: A top-level layout element (paragraph, image, quote, code block, list,
 * collapse region, …). Each block occupies its own vertical slice in the rendered output, and is
 * individually indexed for reading-anchor purposes.
 *
 * - **[InlineSpan]**: Inline formatting within a [ContentBlock.Paragraph] or similar text-bearing
 * block (bold, italic, link, inline image, …).
 *
 * The parser guarantees that every [ContentBlock] contains either structured children (quote, list,
 * collapse) or a flat list of [InlineSpan]s — never raw HTML. Unknown tags are silently dropped;
 * their text content is preserved as [InlineSpan.Text].
 */

// ──────────────────────────────────────────────
//  Block-level nodes
// ──────────────────────────────────────────────

/**
 * A top-level content block produced by the HTML parser.
 *
 * Every block maps to one entry in the block-index used for anchor calculation.
 */
sealed class ContentBlock {

    /**
     * A paragraph of inline-formatted text.
     *
     * May also represent a single `<br>` break if [spans] is empty, though typically the parser
     * merges consecutive `<br>` pairs into paragraph boundaries.
     */
    data class Paragraph(val spans: List<InlineSpan>) : ContentBlock()

    /**
     * A standalone (block-level) image.
     *
     * @property url Image source URL (may be relative to domain).
     * @property alt Optional alt text.
     */
    data class Image(val url: String, val alt: String? = null) : ContentBlock()

    /**
     * A block-quote (`<blockquote>` or `div.quote`).
     *
     * @property children Nested blocks inside the quote.
     */
    data class Quote(val children: List<ContentBlock>) : ContentBlock()

    /**
     * A collapsible / show-hide region.
     *
     * @property title Button label (e.g. "点击展开").
     * @property children Hidden blocks revealed on expand.
     */
    data class Collapse(val title: String, val children: List<ContentBlock>) : ContentBlock()

    /**
     * A fenced code / pre-formatted block.
     *
     * @property code Raw text content.
     */
    data class CodeBlock(val code: String) : ContentBlock()

    /**
     * An ordered or unordered list.
     *
     * @property ordered `true` for `<ol>`, `false` for `<ul>`.
     * @property items Each item is itself a list of blocks (nested content).
     */
    data class ListBlock(val ordered: Boolean, val items: List<List<ContentBlock>>) :
            ContentBlock()

    /** A horizontal rule / divider (`<hr>`). */
    data object HorizontalRule : ContentBlock()
}

// ──────────────────────────────────────────────
//  Inline-level nodes
// ──────────────────────────────────────────────

/**
 * An inline span within a [ContentBlock.Paragraph].
 *
 * Spans may nest (e.g. bold text inside a link).
 */
sealed class InlineSpan {

    /** Plain text with no special formatting. */
    data class Text(val text: String) : InlineSpan()

    /**
     * Styled text (bold, italic, underline, strikethrough, or combinations).
     *
     * @property children Nested spans (allows `<b><i>text</i></b>`).
     * @property style The combined [StyleBundle] for this span.
     */
    data class StyledText(val children: List<InlineSpan>, val style: StyleBundle) : InlineSpan()

    /**
     * A hyperlink.
     *
     * @property href Target URL (absolute or relative).
     * @property children Inline spans forming the link label.
     */
    data class Link(val href: String, val children: List<InlineSpan>) : InlineSpan()

    /**
     * An inline image (inside a paragraph).
     *
     * @property url Image source URL.
     * @property alt Optional alt text.
     */
    data class InlineImage(val url: String, val alt: String? = null) : InlineSpan()

    /** Inline code (`<code>` not inside `<pre>`). */
    data class InlineCode(val code: String) : InlineSpan()

    /** A line break (`<br>`). */
    data object LineBreak : InlineSpan()
}

// ──────────────────────────────────────────────
//  Style bundle
// ──────────────────────────────────────────────

/**
 * Combined inline style properties for a [InlineSpan.StyledText].
 *
 * All properties default to `null` / `false`, meaning "inherit from parent".
 *
 * @property bold `<b>` / `<strong>`.
 * @property italic `<i>` / `<em>`.
 * @property underline `<u>`.
 * @property strikethrough `<s>` / `<del>` / `<strike>`.
 * @property color Foreground color as ARGB [Long] (nullable = inherit).
 * @property fontSize Font size in sp (nullable = inherit).
 * @property fontFamily Font family name (nullable = inherit).
 */
data class StyleBundle(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false,
        val color: Long? = null,
        val fontSize: Float? = null,
        val fontFamily: String? = null
)
