package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.AddFriendOption
import io.github.littlesurvival.dto.page.AddFriendPopoutScreen
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils

class AddFriendPopoutScreenParser : Parser<AddFriendPopoutScreen> {

    override suspend fun parse(html: String): ParseResult<AddFriendPopoutScreen> {
        return try {
            val body = extractCData(html) ?: html
            val doc = Ksoup.parse(body)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))
            if (ParseUtils.isUnDefinedOperation(doc)) return ParseResult.Failure("未定义操作")

            val form = doc.selectFirst("form[id^=addform_]") ?: doc.selectFirst("form[name^=addform_]")
            val uid =
                form?.attr("id")?.let { ADDFORM_ID_RE.find(it)?.groupValues?.get(1)?.toIntOrNull() }
                    ?: form?.selectFirst("input[name=referer]")?.attr("value")?.let { ParseUtils.extractUid(it)?.value }
                    ?: doc.selectFirst("a[href*=space-uid-]")?.attr("href")?.let { ParseUtils.extractUid(it)?.value }
                    ?: return ParseResult.Failure("Cannot parse add friend target user id")

            val avatarUrl = doc.selectFirst(".user_avatar[src]")?.attr("src")?.substringBefore("?")?.ifEmpty { null }
            val username = doc.selectFirst(".tip strong")?.text()?.trim()?.ifEmpty { null } ?: ""
            val availableOptions =
                doc.select("select[name=gid] option").mapNotNull { option ->
                    val id = option.attr("value").trim().toIntOrNull() ?: return@mapNotNull null
                    val reason = option.text().trim()
                    if (reason.isEmpty()) return@mapNotNull null
                    AddFriendOption(id = id, reason = reason)
                }

            ParseResult.Success(
                AddFriendPopoutScreen(
                    user = User(uid = UserId(uid), name = username, avatarUrl = avatarUrl),
                    availableOption = availableOptions
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse add friend popout screen", e)
        }
    }

    companion object {
        private val ADDFORM_ID_RE = Regex("""addform_(\d+)""")

        private fun extractCData(html: String): String? {
            val start = html.indexOf("<![CDATA[")
            if (start < 0) return null
            val contentStart = start + "<![CDATA[".length
            val end = html.indexOf("]]>", contentStart)
            if (end < 0) return null
            return html.substring(contentStart, end)
        }
    }
}
