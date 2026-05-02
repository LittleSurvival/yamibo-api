package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.PrivateMessage
import io.github.littlesurvival.dto.page.PrivateMessagePage
import io.github.littlesurvival.dto.page.PrivateMessageType
import io.github.littlesurvival.dto.value.PrivateMessageId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils

class PrivateMessagePageParser : Parser<PrivateMessagePage> {

    override suspend fun parse(html: String): ParseResult<PrivateMessagePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val pmForm = doc.selectFirst("#pmform") ?: return ParseResult.Failure("Private-message form not found")
            val formAction = pmForm.attr("action")
            val pmId = extractPmId(formAction) ?: return ParseResult.Failure("Private-message id not found")
            val toUser = pmForm.selectFirst("input[name=touid]")?.attr("value")?.toIntOrNull()?.let { UserId(it) }
                ?: ParseUtils.extractToUid(formAction)
                ?: return ParseResult.Failure("Private-message target user not found")

            val title = doc.selectFirst(".header h2")?.text()?.trim().orEmpty()
            val friendName = title
                .removePrefix("正在与")
                .substringBefore("聊天")
                .trim()
                .ifEmpty { "" }
            val selfUid = doc.select("script")
                .asSequence()
                .mapNotNull { ParseUtils.extractUidFromScript(it.data()) ?: ParseUtils.extractUidFromScript(it.html()) }
                .firstOrNull()
                ?: UserId(0)

            val messages = doc.select(".msgbox > .friend_msg, .msgbox > .self_msg")
                .mapNotNull { parseMessage(it, toUser, friendName, selfUid) }

            ParseResult.Success(
                PrivateMessagePage(
                    toUser = toUser,
                    title = title,
                    pmId = pmId,
                    messages = messages,
                    pageNav = ParseUtils.parsePageNav(doc)
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse private-message page", e)
        }
    }

    private fun parseMessage(item: Element, toUser: UserId, friendName: String, selfUid: UserId): PrivateMessage? {
        val type = if (item.hasClass("self_msg")) PrivateMessageType.Self else PrivateMessageType.Friend
        val contentEl = item.selectFirst(".dialog_c") ?: return null
        val avatarUrl = item.selectFirst(".avat img")?.attr("src")?.ifEmpty { null }
        val timeText = item.selectFirst(".date")?.text()?.trim().orEmpty()
        val user = when (type) {
            PrivateMessageType.Self -> User(uid = selfUid, name = "", avatarUrl = avatarUrl)
            PrivateMessageType.Friend -> User(uid = toUser, name = friendName, avatarUrl = avatarUrl)
        }

        return PrivateMessage(
            user = user,
            messageType = type,
            contentHtml = contentEl.html().trim(),
            timeInfo = TimeInfo.parse(timeText)
        )
    }

    companion object {
        private val PM_ID_RE = Regex("[?&]pmid=(\\d+)")

        private fun extractPmId(url: String): PrivateMessageId? {
            return PM_ID_RE.find(url)?.groupValues?.get(1)?.toIntOrNull()?.let { PrivateMessageId(it) }
        }
    }
}
