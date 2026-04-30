package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.ReplyItem
import io.github.littlesurvival.dto.page.ReplyPostItems
import io.github.littlesurvival.dto.page.UserSpaceThreadPage
import io.github.littlesurvival.dto.page.UserSpaceThreadReplyPage
import io.github.littlesurvival.parse.util.ParseUtils

class UserSpaceThreadPageParser : Parser<UserSpaceThreadPage> {

    override suspend fun parse(html: String): ParseResult<UserSpaceThreadPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val threads = mutableListOf<ThreadSummary>()
            val items = doc.select(".threadlist li.list")
            for (item in items) {
                val threadLink = item.selectFirst("a[href*=viewthread]") ?: continue
                val url = threadLink.attr("href")
                val tid = ParseUtils.extractTid(url) ?: continue
                val title = item.selectFirst(".threadlist_tit em")?.text()?.trim() ?: ""
                val tag = item.selectFirst(".threadlist_tit .micon")?.text()?.trim()
                val hasPoll = tag == "投票"

                val authorLink = item.selectFirst(".threadlist_top .muser a")
                val authorUid = authorLink?.attr("href")?.let { ParseUtils.extractUid(it) }
                val authorName = authorLink?.text()?.trim() ?: ""
                val avatarUrl = item.selectFirst(".threadlist_top .mimg img")?.attr("src")?.ifEmpty { null }
                val author =
                    if (authorUid != null) User(uid = authorUid, name = authorName, avatarUrl = avatarUrl) else null

                val description = item.selectFirst(".threadlist_mes")?.text()?.trim()?.ifEmpty { null }

                var forumTag: String? = null
                var fid = ParseUtils.extractFid(url)
                var viewCount: Int? = null
                var replyCount: Int? = null
                for (li in item.select(".threadlist_foot li")) {
                    if (li.hasClass("mr")) {
                        li.selectFirst("a[href*=forumdisplay]")?.attr("href")?.let { forumUrl ->
                            fid = ParseUtils.extractFid(forumUrl) ?: fid
                        }
                        forumTag = li.text().trim().removePrefix("#").ifEmpty { null }
                    } else if (viewCount == null && li.selectFirst("i.dm-eye-fill") != null) {
                        viewCount = li.text().trim().replace(NON_DIGIT_RE, "").toIntOrNull()
                    } else if (replyCount == null && li.selectFirst("i.dm-chat-s-fill") != null) {
                        replyCount = li.text().trim().replace(NON_DIGIT_RE, "").toIntOrNull()
                    }
                }

                val lastUpdateText = item.selectFirst(".muser .mtime")?.text()?.trim()?.ifEmpty { null }
                threads.add(
                    ThreadSummary(
                        tid = tid,
                        title = title,
                        fid = fid,
                        hasPoll = hasPoll,
                        url = url,
                        author = author,
                        description = description,
                        replyCount = replyCount,
                        viewCount = viewCount,
                        tag = forumTag ?: tag,
                        lastUpdate = lastUpdateText?.let { TimeInfo.parse(it) }
                    )
                )
            }

            ParseResult.Success(UserSpaceThreadPage(threads = threads, pageNav = ParseUtils.parsePageNav(doc)))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse user space thread page", e)
        }
    }

    companion object {
        private val NON_DIGIT_RE = Regex("[^0-9]")
    }
}

class UserSpaceThreadReplyPageParser : Parser<UserSpaceThreadReplyPage> {

    override suspend fun parse(html: String): ParseResult<UserSpaceThreadReplyPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val replies = mutableListOf<ReplyItem>()
            val items = doc.select(".threadlist li.list")
            for (item in items) {
                val titleLink = item.selectFirst("a.mt10[href*=findpost]") ?: continue
                val titleUrl = titleLink.attr("href")
                val tId = ParseUtils.extractTid(titleUrl) ?: continue
                val fid = item.selectFirst("a[href*=forumdisplay]")?.attr("href")?.let { ParseUtils.extractFid(it) }
                    ?: ParseUtils.extractFid(titleUrl)
                val title = titleLink.selectFirst(".threadlist_tit em")?.text()?.trim() ?: titleLink.text().trim()

                val posts = item.select("a[href*=findpost][href*=pid=]")
                    .mapNotNull { link ->
                        val url = link.attr("href")
                        val pId = ParseUtils.extractPid(url) ?: return@mapNotNull null
                        ReplyPostItems(
                            tId = ParseUtils.extractTid(url) ?: tId,
                            pId = pId,
                            url = url,
                            quote = link.selectFirst("blockquote")?.text()?.trim() ?: link.text().trim()
                        )
                    }

                replies.add(ReplyItem(title = title, tId = tId, fid = fid, url = titleUrl, posts = posts))
            }

            ParseResult.Success(UserSpaceThreadReplyPage(replies = replies, pageNav = ParseUtils.parsePageNav(doc)))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse user space thread reply page", e)
        }
    }
}
