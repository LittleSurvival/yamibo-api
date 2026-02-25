package io.github.littlesurvival.render.util

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import io.github.littlesurvival.render.ContentBlock
import io.github.littlesurvival.render.FeatureFlags
import io.github.littlesurvival.render.InlineSpan
import io.github.littlesurvival.render.StyleBundle

/**
 * Parses the `contentHtml` of a [Post] into a list of [ContentBlock]s.
 *
 * Design principles:
 * 1. **White-list** — only known tags produce styled output; unknown tags are
 * ```
 *    dropped but their text content is preserved.
 * ```
 * 2. **Never throw** — any parsing error is swallowed and results in a
 * ```
 *    degraded-but-visible output.
 * ```
 * 3. **Feature-flags** — inline colours/sizes/fonts are only honoured when
 * ```
 *    the corresponding [FeatureFlags] field is `true`.
 * ```
 * Supported tags (initial set):
 * - Structure: `<br>`, `<p>`, `<div>`, `<hr>`
 * - Formatting: `<b>`, `<strong>`, `<i>`, `<em>`, `<u>`, `<s>`, `<del>`, `<strike>`, `<font>`,
 * `<span>` (with inline style)
 * - Links: `<a href>`
 * - Images: `<img>` (src / data-original / file attributes)
 * - Quote: `<blockquote>`, `<div class="quote">`
 * - Collapse: `<div class="spoiler">`, Discuz `showhide`
 * - Code: `<pre>`, `<code>`
 * - Lists: `<ul>`, `<ol>`, `<li>`
 */
object HtmlContentParser {

    // Tags whose immediate children should be treated as block-level.
    private val BLOCK_CONTAINER_TAGS =
            setOf(
                    "div",
                    "blockquote",
                    "article",
                    "section",
                    "main",
                    "aside",
                    "header",
                    "footer",
                    "figure",
                    "figcaption",
                    "details",
                    "summary"
            )

    // Tags that produce styled inline spans.
    private val INLINE_STYLE_TAGS =
            setOf("b", "strong", "i", "em", "u", "s", "del", "strike", "font", "span", "sup", "sub")

    // Tags treated as inline containers (their children become inline).
    private val INLINE_CONTAINER_TAGS =
            INLINE_STYLE_TAGS + setOf("a", "code", "label", "abbr", "mark")

    /**
     * Parse an HTML fragment into a flat list of [ContentBlock]s.
     *
     * @param html Raw HTML string (the post's `contentHtml`).
     * @param flags Current feature flags controlling style extraction.
     * @return Ordered list of content blocks. Never empty — worst case a
     * ```
     *         single empty [ContentBlock.Paragraph].
     * ```
     */
    fun parse(html: String, flags: FeatureFlags = FeatureFlags()): List<ContentBlock> {
        if (html.isBlank()) return listOf(ContentBlock.Paragraph(emptyList()))
        return try {
            val body = Ksoup.parse(html).body()
            val blocks = parseChildren(body, flags)
            if (blocks.isEmpty()) listOf(ContentBlock.Paragraph(emptyList())) else blocks
        } catch (_: Exception) {
            // Fallback: treat everything as a single text paragraph
            listOf(ContentBlock.Paragraph(listOf(InlineSpan.Text(html))))
        }
    }

    // ── Block-level parsing ─────────────────────────────────────────

    /** Recursively parse the children of [parent] into blocks. */
    private fun parseChildren(parent: Element, flags: FeatureFlags): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        val pendingInlines = mutableListOf<InlineSpan>()

        fun flushInlines() {
            if (pendingInlines.isNotEmpty()) {
                blocks.add(ContentBlock.Paragraph(ArrayList(pendingInlines)))
                pendingInlines.clear()
            }
        }

        for (child in parent.childNodes()) {
            when {
                child is TextNode -> {
                    val text = child.getWholeText()
                    if (text.isNotBlank()) {
                        pendingInlines.add(InlineSpan.Text(text))
                    } else if (text.isNotEmpty() && pendingInlines.isNotEmpty()) {
                        // Preserve single whitespace between inline elements
                        pendingInlines.add(InlineSpan.Text(" "))
                    }
                }
                child is Element -> {
                    val tag = child.tagName().lowercase()
                    when {
                        tag == "br" -> {
                            pendingInlines.add(InlineSpan.LineBreak)
                        }
                        tag == "hr" -> {
                            flushInlines()
                            blocks.add(ContentBlock.HorizontalRule)
                        }
                        tag == "p" -> {
                            flushInlines()
                            val innerSpans = parseInlineChildren(child, flags)
                            if (innerSpans.isNotEmpty()) {
                                blocks.add(ContentBlock.Paragraph(innerSpans))
                            }
                        }
                        tag == "img" -> {
                            val url = extractImageUrl(child)
                            if (url != null && !isSmileyOrCommon(url)) {
                                flushInlines()
                                blocks.add(
                                        ContentBlock.Image(
                                                url = url,
                                                alt = child.attr("alt").ifEmpty { null }
                                        )
                                )
                            }
                        }
                        tag == "blockquote" || isQuoteDiv(child) -> {
                            flushInlines()
                            blocks.add(ContentBlock.Quote(parseChildren(child, flags)))
                        }
                        isCollapseDiv(child) -> {
                            flushInlines()
                            val title = extractCollapseTitle(child)
                            blocks.add(ContentBlock.Collapse(title, parseChildren(child, flags)))
                        }
                        tag == "pre" -> {
                            flushInlines()
                            blocks.add(ContentBlock.CodeBlock(child.text()))
                        }
                        tag == "ul" || tag == "ol" -> {
                            flushInlines()
                            val items =
                                    child.select("> li").map { li ->
                                        parseChildren(li, flags).ifEmpty {
                                            val spans = parseInlineChildren(li, flags)
                                            if (spans.isNotEmpty())
                                                    listOf(ContentBlock.Paragraph(spans))
                                            else emptyList()
                                        }
                                    }
                            blocks.add(ContentBlock.ListBlock(ordered = tag == "ol", items = items))
                        }
                        tag == "table" -> {
                            // Tables are complex; for now extract text
                            flushInlines()
                            val text = child.text().trim()
                            if (text.isNotEmpty()) {
                                blocks.add(ContentBlock.Paragraph(listOf(InlineSpan.Text(text))))
                            }
                        }
                        tag == "div" -> {
                            // Generic div — recurse as block container
                            flushInlines()
                            blocks.addAll(parseChildren(child, flags))
                        }
                        tag in INLINE_CONTAINER_TAGS || tag in INLINE_STYLE_TAGS -> {
                            // Inline element — collect spans
                            pendingInlines.addAll(parseInlineElement(child, flags))
                        }
                        tag == "a" -> {
                            pendingInlines.addAll(parseInlineElement(child, flags))
                        }
                        tag == "code" -> {
                            // Inline code
                            pendingInlines.add(InlineSpan.InlineCode(child.text()))
                        }
                        tag in BLOCK_CONTAINER_TAGS -> {
                            flushInlines()
                            blocks.addAll(parseChildren(child, flags))
                        }
                        else -> {
                            // Unknown tag — preserve text content as inline
                            val text = child.text().trim()
                            if (text.isNotEmpty()) {
                                pendingInlines.add(InlineSpan.Text(text))
                            }
                        }
                    }
                }
            }
        }
        flushInlines()

        // Post-process: merge consecutive LineBreak-only paragraphs
        return coalesceParagraphs(blocks)
    }

    // ── Inline-level parsing ────────────────────────────────────────

    /** Parse all children of [element] as inline spans. */
    private fun parseInlineChildren(element: Element, flags: FeatureFlags): List<InlineSpan> {
        val spans = mutableListOf<InlineSpan>()
        for (child in element.childNodes()) {
            when {
                child is TextNode -> {
                    val text = child.getWholeText()
                    if (text.isNotEmpty()) {
                        spans.add(InlineSpan.Text(text))
                    }
                }
                child is Element -> {
                    spans.addAll(parseInlineElement(child, flags))
                }
            }
        }
        return spans
    }

    /** Parse a single inline [Element] (and its descendants) into spans. */
    private fun parseInlineElement(element: Element, flags: FeatureFlags): List<InlineSpan> {
        val tag = element.tagName().lowercase()

        return when {
            tag == "br" -> listOf(InlineSpan.LineBreak)
            tag == "img" -> {
                val url = extractImageUrl(element)
                if (url != null && !isSmileyOrCommon(url)) {
                    listOf(InlineSpan.InlineImage(url, element.attr("alt").ifEmpty { null }))
                } else {
                    emptyList()
                }
            }
            tag == "a" -> {
                val href = element.attr("href")
                val children = parseInlineChildren(element, flags)
                if (href.isNotBlank()) {
                    listOf(InlineSpan.Link(href, children))
                } else {
                    children
                }
            }
            tag == "code" -> {
                listOf(InlineSpan.InlineCode(element.text()))
            }
            tag in INLINE_STYLE_TAGS -> {
                val attrs = element.attributes().associate { it.key to it.value }
                val style =
                        StyleExtractor.extract(
                                tagName = tag,
                                attrs = attrs,
                                inlineStyle = element.attr("style").ifEmpty { null },
                                flags = flags
                        )
                val children = parseInlineChildren(element, flags)
                if (style != StyleBundle() && children.isNotEmpty()) {
                    listOf(InlineSpan.StyledText(children, style))
                } else {
                    children
                }
            }
            else -> {
                // Unknown inline — preserve text
                val text = element.text()
                if (text.isNotEmpty()) listOf(InlineSpan.Text(text)) else emptyList()
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Extract the best image URL from an `<img>` element.
     *
     * Tries `data-original` (lazy-load), then `file`, then `src`.
     */
    private fun extractImageUrl(img: Element): String? {
        val dataOriginal = img.attr("data-original").ifEmpty { null }
        if (dataOriginal != null) return dataOriginal

        val file = img.attr("file").ifEmpty { null }
        if (file != null) return file

        val src = img.attr("src").ifEmpty { null }
        return src
    }

    /** Return `true` if the URL looks like a forum smiley / common icon. */
    private fun isSmileyOrCommon(url: String): Boolean {
        return url.contains("static/image/smiley") || url.contains("static/image/common")
    }

    /** Check if [el] is a `<div class="quote">` variant. */
    private fun isQuoteDiv(el: Element): Boolean {
        if (el.tagName().lowercase() != "div") return false
        val cls = el.className().lowercase()
        return "quote" in cls
    }

    /** Check if [el] is a collapse / spoiler / showhide div. */
    private fun isCollapseDiv(el: Element): Boolean {
        if (el.tagName().lowercase() != "div") return false
        val cls = el.className().lowercase()
        return "spoiler" in cls || "showhide" in cls || "collapse" in cls
    }

    /** Extract the collapse title/button text from a collapse container. */
    private fun extractCollapseTitle(el: Element): String {
        // Common patterns: <div class="spoiler_title">Click to show</div>
        val titleEl = el.select(".spoiler_title, .showhide_title, .collapse_title").first()
        return titleEl?.text()?.trim() ?: "点击展开"
    }

    /**
     * Merge consecutive paragraphs that consist solely of [InlineSpan.LineBreak] or are empty into
     * a single line-break paragraph.
     */
    private fun coalesceParagraphs(blocks: List<ContentBlock>): List<ContentBlock> {
        if (blocks.size <= 1) return blocks
        val result = mutableListOf<ContentBlock>()
        for (block in blocks) {
            if (block is ContentBlock.Paragraph && block.spans.isEmpty()) {
                // Skip empty paragraphs
                continue
            }
            // Merge paragraph that is only line-breaks with previous
            if (block is ContentBlock.Paragraph &&
                            block.spans.all { it is InlineSpan.LineBreak } &&
                            result.lastOrNull() is ContentBlock.Paragraph
            ) {
                // Append a single line break to previous paragraph
                val prev = result.removeAt(result.lastIndex) as ContentBlock.Paragraph
                result.add(ContentBlock.Paragraph(prev.spans + InlineSpan.LineBreak))
                continue
            }
            result.add(block)
        }
        return result
    }
}
