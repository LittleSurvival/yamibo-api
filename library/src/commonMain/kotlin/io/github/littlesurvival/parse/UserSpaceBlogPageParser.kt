package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.BlogSummary
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.UserSpaceBlogPage
import io.github.littlesurvival.parse.util.ParseUtils

class UserSpaceBlogPageParser : Parser<UserSpaceBlogPage> {

    override suspend fun parse(html: String): ParseResult<UserSpaceBlogPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val blogs = mutableListOf<BlogSummary>()
            val items = doc.select(".threadlist li.list")
            for (item in items) {
                val blogLink = item.selectFirst("a[href*=do=blog][href*=id=]") ?: continue
                val url = blogLink.attr("href")
                val bId = ParseUtils.extractBid(url) ?: continue
                val title = blogLink.selectFirst(".threadlist_tit")?.text()?.trim() ?: ""
                val description = blogLink.selectFirst(".threadlist_mes")?.text()?.trim() ?: ""

                val authorLink = item.selectFirst(".threadlist_top .muser a.mmc")
                    ?: item.selectFirst(".threadlist_top a.mmc")
                    ?: continue
                val authorUid = authorLink.attr("href").let { ParseUtils.extractUid(it) } ?: continue
                val avatarUrl = item.selectFirst(".threadlist_top .mimg img")?.attr("src")?.ifEmpty { null }
                val author = User(uid = authorUid, name = authorLink.text().trim(), avatarUrl = avatarUrl)

                val timeText = item.selectFirst(".mtime span")?.text()?.trim()
                    ?: item.selectFirst(".mtime")?.text()?.trim()
                    ?: ""

                blogs.add(
                    BlogSummary(
                        title = title,
                        bId = bId,
                        url = url,
                        description = description,
                        author = author,
                        timeInfo = TimeInfo.parse(timeText)
                    )
                )
            }

            ParseResult.Success(UserSpaceBlogPage(blogs = blogs, pageNav = ParseUtils.parsePageNav(doc)))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse user space blog page", e)
        }
    }
}
