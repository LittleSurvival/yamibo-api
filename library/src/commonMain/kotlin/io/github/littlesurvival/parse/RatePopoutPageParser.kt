package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.RatePopoutPage
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import io.github.littlesurvival.parse.util.ParseUtils

class RatePopoutPageParser : Parser<RatePopoutPage> {

    override suspend fun parse(html: String): ParseResult<RatePopoutPage> {
        return try {
            val body = extractCData(html) ?: html
            val doc = Ksoup.parse(body)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))
            if (ParseUtils.isUnDefinedOperation(doc)) return ParseResult.Failure("未定义操作")

            val availableScores = doc.select("select#rate1 option")
                .mapNotNull { it.attr("value").ifEmpty { it.text() }.trim().toIntOrNull() }

            val defaultReasons = doc.select("select#reason option")
                .map { it.attr("value").ifEmpty { it.text() }.trim() }
                .filter { it.isNotEmpty() }

            if (availableScores.isEmpty() && defaultReasons.isEmpty()) {
                PostResponseUtils.parseMessageText(html)?.let { message ->
                    return ParseResult.Failure(message)
                }
            }

            ParseResult.Success(
                RatePopoutPage(
                    availableScores = availableScores,
                    defaultReasons = defaultReasons
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse rate popout page", e)
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
