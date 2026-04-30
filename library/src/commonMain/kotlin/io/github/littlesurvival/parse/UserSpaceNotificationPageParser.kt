package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.NoticeItem
import io.github.littlesurvival.dto.page.NoticeType
import io.github.littlesurvival.dto.page.PrivateMessageItem
import io.github.littlesurvival.dto.page.UserSpaceNoticePage
import io.github.littlesurvival.dto.page.UserSpacePrivateMessagePage
import io.github.littlesurvival.dto.value.NoticeId
import io.github.littlesurvival.parse.util.ParseUtils

class UserSpacePrivateMessagePageParser : Parser<UserSpacePrivateMessagePage> {

    override suspend fun parse(html: String): ParseResult<UserSpacePrivateMessagePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val unreadCount = doc.selectFirst(".dhnv a.mon strong")
                ?.text()
                ?.let { UNREAD_COUNT_RE.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            val messages = mutableListOf<PrivateMessageItem>()
            val items = doc.select("#pmlist li")
            for (item in items) {
                val conversationLink = item.selectFirst(".mimg a[href*=touid=]")
                    ?: item.selectFirst("a[href*=touid=]")
                    ?: continue
                val conversationUrl = conversationLink.attr("href")
                val uid = ParseUtils.extractToUid(conversationUrl) ?: continue
                val avatarUrl = conversationLink.selectFirst("img")?.attr("src")?.ifEmpty { null }

                val titleEl = item.selectFirst(".mtit") ?: continue
                val timeText = titleEl.selectFirst(".mtime")?.text()?.trim() ?: ""
                val itemUnreadCount = titleEl.selectFirst(".mnum")?.text()?.trim()?.toIntOrNull()
                titleEl.selectFirst(".mtime")?.remove()
                titleEl.selectFirst(".mnum")?.remove()
                val title = titleEl.text().trim()
                val name = title.substringBefore(" 对我").substringBefore(" 對我").trim().ifEmpty { title }
                val message = item.selectFirst(".mtxt")?.text()?.trim() ?: ""

                messages.add(
                    PrivateMessageItem(
                        user = User(uid = uid, name = name, avatarUrl = avatarUrl),
                        conversationUrl = conversationUrl,
                        timeInfo = TimeInfo.parse(timeText),
                        title = title,
                        message = message,
                        unreadCount = itemUnreadCount
                    )
                )
            }

            ParseResult.Success(
                UserSpacePrivateMessagePage(
                    messages = messages,
                    unreadCount = unreadCount,
                    pageNav = ParseUtils.parsePageNav(doc)
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse user space private message page", e)
        }
    }

    companion object {
        private val UNREAD_COUNT_RE = Regex("(\\d+)")
    }
}

class UserSpaceNoticePageParser : Parser<UserSpaceNoticePage> {

    override suspend fun parse(html: String): ParseResult<UserSpaceNoticePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val notices = mutableListOf<NoticeItem>()
            val items = doc.select("#notice_ul li[notice]")
            for (item in items) {
                val noticeId = item.attr("notice").toIntOrNull()?.let { NoticeId(it) } ?: continue
                val ignoreLink = item.selectFirst("a[id^=a_note_]")
                val ignoreUrl = ignoreLink?.attr("href")?.ifEmpty { null }
                val type = parseNoticeType(ignoreUrl)
                val avatarUrl = item.selectFirst(".mimg img")?.attr("src")?.ifEmpty { null }

                val timeText = item.selectFirst(".mtit span")?.text()?.trim() ?: ""
                val bodyEl = item.selectFirst(".mbody") ?: continue
                val quote = item.selectFirst("blockquote")?.text()?.trim()?.ifEmpty { null }
                val quoteHtml = item.selectFirst(".quote")?.outerHtml()?.trim()
                val contentHtml = listOfNotNull(bodyEl.html().trim(), quoteHtml)
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")

                notices.add(
                    NoticeItem(
                        noticeId = noticeId,
                        type = type,
                        avatarUrl = avatarUrl,
                        timeInfo = TimeInfo.parse(timeText),
                        contentHtml = contentHtml,
                        ignoreUrl = ignoreUrl,
                        quote = quote
                    )
                )
            }

            ParseResult.Success(
                UserSpaceNoticePage(
                    notices = notices,
                    pageNav = ParseUtils.parsePageNav(doc)
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse user space notice page", e)
        }
    }

    private fun parseNoticeType(ignoreUrl: String?): NoticeType {
        return when (NOTICE_TYPE_RE.find(ignoreUrl.orEmpty())?.groupValues?.get(1)) {
            "post" -> NoticeType.Post
            "pcomment" -> NoticeType.PostComment
            "rate" -> NoticeType.Rate
            "friend" -> NoticeType.Friend
            "system" -> NoticeType.System
            else -> NoticeType.Unknown
        }
    }

    companion object {
        private val NOTICE_TYPE_RE = Regex("[?&]type=([^&]+)")
    }
}
