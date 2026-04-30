package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.BlogComment
import io.github.littlesurvival.dto.page.BlogInfo
import io.github.littlesurvival.dto.page.BlogPage
import io.github.littlesurvival.dto.value.BlogCommentId
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils

class BlogPageParser : Parser<BlogPage> {

    override suspend fun parse(html: String): ParseResult<BlogPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val rootEl = doc.selectFirst(".viewthread > .plc") ?: return ParseResult.Failure("Blog root content not found")
            val blogId = parseBlogId(doc, rootEl) ?: BlogId(0)

            val titleEl = doc.selectFirst(".view_tit")
            val categoryText = titleEl?.selectFirst("em")?.text()?.trim()?.ifEmpty { null }
            val title = titleEl?.text()?.trim()?.let { fullTitle ->
                categoryText?.let { fullTitle.removePrefix(it).trim() } ?: fullTitle
            }.orEmpty()

            val stats = rootEl.selectFirst(".mtime .y")?.select("em")
            val totalViews = stats?.getOrNull(0)?.text()?.trim()?.toIntOrNull()
            val totalReplies = stats?.getOrNull(1)?.text()?.trim()?.toIntOrNull()

            val blogInfo = BlogInfo(
                blogId = blogId,
                title = title,
                totalReplies = totalReplies,
                totalViews = totalViews,
                collectUrl = rootEl.selectFirst("a[href*=ac=favorite][href*=type=blog]")?.attr("href")?.ifEmpty { null },
                shareUrl = rootEl.selectFirst("a[href*=ac=share][href*=type=blog]")?.attr("href")?.ifEmpty { null },
                inviteUrl = rootEl.selectFirst("a[href*=action=blog][href*=mod=invite]")?.attr("href")?.ifEmpty { null }
            )

            val rootBlog = BlogComment(
                bcId = null,
                author = parseRootAuthor(rootEl),
                contentHtml = rootEl.selectFirst(".message")?.html()?.trim().orEmpty(),
                replyUrl = null,
                timeInfo = TimeInfo.parse(rootEl.selectFirst(".mtime")?.ownText()?.trim().orEmpty())
            )

            val comments = doc.select(".doing_list_box li[id^=comment_]")
                .mapNotNull { parseComment(it) }

            ParseResult.Success(
                BlogPage(
                    blogInfo = blogInfo,
                    rootBlog = rootBlog,
                    blogComments = comments,
                    pageNav = ParseUtils.parsePageNav(doc)
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse blog page", e)
        }
    }

    private fun parseBlogId(doc: Element, rootEl: Element): BlogId? {
        val canonicalUrl = doc.selectFirst("link[rel=canonical]")?.attr("href")
        if (canonicalUrl != null) ParseUtils.extractBid(canonicalUrl)?.let { return it }

        rootEl.selectFirst("a[href*=type=blog][href*=id=]")?.attr("href")
            ?.let { ParseUtils.extractBid(it) }
            ?.let { return it }

        return doc.selectFirst("form[id^=quickcommentform_] input[name=id]")
            ?.attr("value")
            ?.toIntOrNull()
            ?.let { BlogId(it) }
    }

    private fun parseRootAuthor(rootEl: Element): User {
        val authorLink = rootEl.selectFirst(".authi .z a")
        val uid = authorLink?.attr("href")?.let { ParseUtils.extractUid(it) } ?: UserId(0)
        val name = authorLink?.text()?.trim().orEmpty()
        val avatarUrl = rootEl.selectFirst(".avatar img")?.attr("src")?.ifEmpty { null }
        return User(uid = uid, name = name, avatarUrl = avatarUrl)
    }

    private fun parseComment(item: Element): BlogComment? {
        val commentEl = item.selectFirst(".do_comment") ?: return null
        val bcId = parseCommentId(item, commentEl) ?: return null
        val authorLink = item.selectFirst(".muser a[id^=author_], .muser a[href*=uid=]")
        val uid = authorLink?.attr("href")?.let { ParseUtils.extractUid(it) } ?: UserId(0)
        val name = authorLink?.text()?.trim().orEmpty()
        val avatarUrl = item.selectFirst(".avatar img, .user_avatar")?.attr("src")?.ifEmpty { null }
        val replyUrl = item.selectFirst("a[id^=c_][id$=_reply]")?.attr("href")?.ifEmpty { null }
        val timeText = item.selectFirst(".mtime span")?.text()?.trim().orEmpty()

        return BlogComment(
            bcId = bcId,
            author = User(uid = uid, name = name, avatarUrl = avatarUrl),
            contentHtml = commentEl.html().trim(),
            replyUrl = replyUrl,
            timeInfo = TimeInfo.parse(timeText)
        )
    }

    private fun parseCommentId(item: Element, commentEl: Element): BlogCommentId? {
        ParseUtils.extractBlogCommentId(commentEl.attr("id"))?.let { return it }
        ParseUtils.extractBlogCommentId(item.attr("id"))?.let { return it }
        return item.selectFirst("a[href*=cid=]")?.attr("href")?.let { ParseUtils.extractBlogCommentId(it) }
    }
}
