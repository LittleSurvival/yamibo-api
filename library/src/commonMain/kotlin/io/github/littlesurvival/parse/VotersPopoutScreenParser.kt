package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.VotersPollOption
import io.github.littlesurvival.dto.page.VotersPopoutScreen
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.parse.util.ParseUtils

class VotersPopoutScreenParser : Parser<VotersPopoutScreen> {

    override suspend fun parse(html: String): ParseResult<VotersPopoutScreen> {
        return try {
            val body = extractCData(html) ?: html
            val doc = Ksoup.parse(body)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))
            if (ParseUtils.isUnDefinedOperation(doc)) return ParseResult.Failure("未定义操作")

            val optionElements = doc.select("select#polloptionid option")
            val pollOptions = optionElements.mapNotNull { option ->
                val id = option.attr("value").trim().toIntOrNull() ?: return@mapNotNull null
                val name = option.text().trim()
                if (name.isEmpty()) return@mapNotNull null
                VotersPollOption(id = PollOptionId(id), name = name)
            }
            if (pollOptions.isEmpty()) {
                return ParseResult.Failure(
                    ParseUtils.parsePromptMessageOrNull(doc) ?: "Cannot parse voter poll options"
                )
            }

            val selectedPollOptionId = optionElements
                .firstOrNull { it.hasAttr("selected") }
                ?.attr("value")
                ?.trim()
                ?.toIntOrNull()
                ?.let(::PollOptionId)
                ?: return ParseResult.Failure("Cannot parse selected voter poll option")

            val voters = doc.select("ul.post_box.flex-box.flex-wrap a[href*=uid-]").mapNotNull { anchor ->
                val userId = ParseUtils.extractUid(anchor.attr("href")) ?: return@mapNotNull null
                val name = anchor.text().trim()
                if (name.isEmpty()) return@mapNotNull null
                User(uid = userId, name = name)
            }

            ParseResult.Success(
                VotersPopoutScreen(
                    pollOptions = pollOptions,
                    selectedPollOptionId = selectedPollOptionId,
                    voters = voters
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse voters popout screen", e)
        }
    }

    companion object {
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
