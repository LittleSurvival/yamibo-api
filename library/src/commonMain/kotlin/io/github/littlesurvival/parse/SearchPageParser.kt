package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.parse.util.ParseUtils

class SearchPageParser : Parser<SearchPage> {

    override suspend fun parse(html: String): ParseResult<SearchPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn

            // Query
            val query = doc.select(".threadlist_box h2 em .emfont").first()?.text() ?: ""

            // Total result count
            val totalCount =
                doc.select(".threadlist_box h2 em").first()?.text()?.let {
                    Regex("(\\d+)\\s*个").find(it)?.groupValues?.get(1)?.toIntOrNull()
                }
                    ?: 0

            // Search ID
            val searchIdValue =
                doc.select(".pg a").firstNotNullOfOrNull {
                    Regex("searchid=(\\d+)")
                        .find(it.attr("href"))
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                }
                    ?: Regex("searchid=(\\d+)")
                        .find(html)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
            val searchId = searchIdValue?.let { SearchId(it) }

            // Thread list
            val threads = mutableListOf<ThreadSummary>()
            val items = doc.select(".threadlist li.list")
            for (item in items) {
                // Thread URL and tid
                val threadLink = item.select("a[href*=viewthread]").first() ?: continue
                val threadUrl = threadLink.attr("href")
                val tid = ParseUtils.extractTid(threadUrl) ?: continue

                // Title: <em> inside .threadlist_tit
                val title = item.select(".threadlist_tit em").first()?.text()?.trim() ?: ""

                // Special tag (e.g. "投票")
                val tag = item.select(".threadlist_tit .micon").first()?.text()?.trim()

                // Author info
                val authorLink = item.select(".threadlist_top .muser h3 a.mmc").first()
                val authorName = authorLink?.text()?.trim() ?: ""
                val authorUid = authorLink?.attr("href")?.let { ParseUtils.extractUid(it) }
                val avatarUrl = item.select(".threadlist_top .mimg img").first()?.attr("src")

                val author =
                    if (authorUid != null) {
                        User(uid = authorUid, name = authorName, avatarUrl = avatarUrl)
                    } else null

                // Time
                val timeText = item.select(".muser .mtime").first()?.text()?.trim()

                // Description / preview
                val description =
                    item.select(".threadlist_mes").first()?.text()?.trim()?.ifEmpty { null }

                // Forum tag (e.g. "動漫區")
                val forumTag =
                    item.select(".threadlist_foot li.mr a")
                        .first()
                        ?.text()
                        ?.trim()
                        ?.removePrefix("#")

                // View count
                val viewCount =
                    item.select(".threadlist_foot li").let { lis ->
                        lis
                            .find { it.select("i.dm-eye-fill").isNotEmpty() }
                            ?.text()
                            ?.trim()
                            ?.replace(Regex("[^0-9]"), "")
                            ?.toIntOrNull()
                    }

                // Reply count
                val replyCount =
                    item.select(".threadlist_foot li").let { lis ->
                        lis
                            .find { it.select("i.dm-chat-s-fill").isNotEmpty() }
                            ?.text()
                            ?.trim()
                            ?.replace(Regex("[^0-9]"), "")
                            ?.toIntOrNull()
                    }

                threads.add(
                    ThreadSummary(
                        tid = tid,
                        title = title,
                        url = threadUrl,
                        author = author,
                        description = description,
                        replyCount = replyCount,
                        viewCount = viewCount,
                        tag = forumTag ?: tag,
                        lastUpdateText = timeText
                    )
                )
            }

            // Pagination
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(
                SearchPage(
                    query = query,
                    searchId = searchId,
                    threads = threads,
                    totalCount = totalCount,
                    pageNav = pageNav
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse search page", e)
        }
    }
}
