package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.*
import io.github.littlesurvival.parse.util.ParseUtils

class ForumPageParser : Parser<ForumPage> {

    override suspend fun parse(html: String): ParseResult<ForumPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn

            // --- Forum info ---
            val headerH2 = doc.select(".forumdisplay-top h2").first()
            val forumName = headerH2?.ownText()?.trim() ?: ""
            val newThreadLink = doc.select("#a_newthread").attr("href")
            val fid = ParseUtils.extractFid(newThreadLink) ?: ForumId(0)

            val forum = ForumInfo(fid = fid, name = forumName)

            // --- Sub forums ---
            val subForums = mutableListOf<ForumSummary>()
            val subForumItems = doc.select(".forumlist .sub-forum li")
            for (li in subForumItems) {
                val linkEl = li.select("a[href*=forumdisplay]").first() ?: continue
                val url = linkEl.attr("href")
                val sfid = ParseUtils.extractFid(url) ?: continue
                val sfName = li.select(".mtit").text().trim()
                val iconUrl = li.select(".micon img").attr("src").ifEmpty { null }
                subForums.add(ForumSummary(fid = sfid, name = sfName, url = url, iconUrl = iconUrl))
            }

            // --- Pinned items ---
            val pinnedItems = mutableListOf<PinnedItem>()
            val pinnedEls = doc.select(".threadlist li.list_top")
            for (el in pinnedEls) {
                val linkEl = el.select("a").first() ?: continue
                val url = linkEl.attr("href")
                val iconSpan = el.select(".micon")
                val iconText = iconSpan.text().trim()

                if (iconText == "公告" || iconSpan.hasClass("gonggao")) {
                    val title = linkEl.text().replace(iconText, "").trim()
                    pinnedItems.add(PinnedItem.Announcement(title = title, url = url))
                } else {
                    val tid = ParseUtils.extractTid(url)
                    val title = el.select("em").text().trim()
                    if (tid != null) {
                        pinnedItems.add(PinnedItem.Thread(tid = tid, title = title, url = url))
                    }
                }
            }

            // --- Thread list ---
            val threads = mutableListOf<ThreadSummary>()
            val threadEls = doc.select(".threadlist li.list")
            for (el in threadEls) {
                val titleLink = el.select("a[href*=viewthread]").first() ?: continue
                val url = titleLink.attr("href")
                val tid = ParseUtils.extractTid(url) ?: continue
                val title = el.select(".threadlist_tit em").text().trim()

                // Author
                val authorLink = el.select(".threadlist_top .muser a").first()
                val authorUid = authorLink?.attr("href")?.let { ParseUtils.extractUid(it) }
                val authorName = authorLink?.text()?.trim() ?: ""
                val avatarUrl = el.select(".threadlist_top .mimg img").attr("src").ifEmpty { null }
                val author =
                        if (authorUid != null) {
                            User(uid = authorUid, name = authorName, avatarUrl = avatarUrl)
                        } else null

                // Description
                val description = el.select(".threadlist_mes").text().trim().ifEmpty { null }

                // Counts
                val footItems = el.select(".threadlist_foot li")
                var viewCount: Int? = null
                var replyCount: Int? = null
                var tag: String? = null
                for (footItem in footItems) {
                    if (footItem.hasClass("mr")) {
                        tag = footItem.text().trim().removePrefix("#").ifEmpty { null }
                    } else if (footItem.select(".dm-eye-fill").isNotEmpty()) {
                        viewCount =
                                footItem.ownText().trim().toIntOrNull()
                                        ?: footItem.text()
                                                .trim()
                                                .filter { it.isDigit() }
                                                .toIntOrNull()
                    } else if (footItem.select(".dm-chat-s-fill").isNotEmpty()) {
                        replyCount =
                                footItem.ownText().trim().toIntOrNull()
                                        ?: footItem.text()
                                                .trim()
                                                .filter { it.isDigit() }
                                                .toIntOrNull()
                    }
                }

                // Last update time
                val lastUpdateText = el.select(".muser .mtime").text().trim().ifEmpty { null }

                threads.add(
                    ThreadSummary(
                        tid = tid,
                        title = title,
                        url = url,
                        author = author,
                        description = description,
                        replyCount = replyCount,
                        viewCount = viewCount,
                        tag = tag,
                        lastUpdateText = lastUpdateText
                    )
                )
            }

            // --- Pagination ---
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(
                    ForumPage(
                            forum = forum,
                            pinnedItems = pinnedItems,
                            subForums = subForums,
                            threads = threads,
                            pageNav = pageNav
                    )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse forum page", e)
        }
    }
}
