package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.page.RateResultItem
import io.github.littlesurvival.dto.page.RateResultPopoutPage
import io.github.littlesurvival.parse.util.ParseUtils

class RateResultPopoutPageParser : Parser<RateResultPopoutPage> {

    override suspend fun parse(html: String): ParseResult<RateResultPopoutPage> {
        return try {
            val body = extractCData(html) ?: html
            val doc = Ksoup.parse(body)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))
            if (ParseUtils.isUnDefinedOperation(doc)) return ParseResult.Failure("未定义操作")

            val page = parsePage(doc)
            if (page.rates.isEmpty()) {
                parseFailureMessage(doc, html)?.let { message ->
                    return ParseResult.Failure(message)
                }
            }

            ParseResult.Success(page)
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse rate result popout page", e)
        }
    }

    private fun parsePage(doc: Document): RateResultPopoutPage {
        val rates = mutableListOf<RateResultItem>()
        var totalScore: Int? = null
        var pendingReasonIndex: Int? = null

        for (row in doc.select("li.flex-box.mli")) {
            if (isHeaderRow(row)) continue

            parseScoreRow(row)?.let { item ->
                rates.add(item)
                pendingReasonIndex = rates.lastIndex
                continue
            }

            val reason = parseReasonRow(row)
            if (reason != null && pendingReasonIndex != null) {
                val current = rates[pendingReasonIndex]
                if (current.reason == null) {
                    rates[pendingReasonIndex] = current.copy(reason = reason)
                }
                continue
            }
        }

        totalScore = parseTotalScore(doc)

        return RateResultPopoutPage(
            totalScore = totalScore,
            rates = rates,
            pageNav = ParseUtils.parsePageNav(doc)
        )
    }

    private fun parseScoreRow(row: Element): RateResultItem? {
        val scoreText = row.selectFirst(".flex-2.xs1.xg1 .z")?.text()?.trim() ?: return null
        val userName = row.select(".flex-2.xs1.xg1 .z").getOrNull(1)?.text()?.trim() ?: return null
        val timeText = row.selectFirst(".flex-3.xs1.xg1 .y")?.text()?.trim() ?: return null
        val score = SCORE_RE.find(scoreText)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return RateResultItem(
            score = score,
            userName = userName,
            timeInfo = TimeInfo.parse(timeText)
        )
    }

    private fun parseReasonRow(row: Element): String? {
        return row.selectFirst(".flex.xs1.xg1 .z")?.text()?.trim()?.ifEmpty { null }
    }

    private fun parseTotalScore(doc: Document): Int? {
        val totalText = doc.selectFirst(".o.pns")?.text()?.trim().orEmpty()
        return TOTAL_SCORE_RE.find(totalText)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun isHeaderRow(row: Element): Boolean {
        val text = row.text().trim()
        return text.contains("积分") && text.contains("用户名") && text.contains("时间")
    }

    private fun parseFailureMessage(doc: Document, html: String): String? {
        ParseUtils.parsePromptMessageOrNull(doc)?.let { return it }
        return parseCDataMessage(html)
    }

    private fun parseCDataMessage(html: String): String? {
        val body = extractCData(html) ?: html
        val doc = Ksoup.parse(body)
        return doc.selectFirst(".jump_c p")?.text()?.trim()?.ifEmpty { null }
            ?: doc.selectFirst("#messagetext p")?.text()?.trim()?.ifEmpty { null }
    }

    companion object {
        private val SCORE_RE = Regex("积分\\s*([+-]?\\d+)\\s*点")
        private val TOTAL_SCORE_RE = Regex("积分\\s*([+-]?\\d+)\\s*点")

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
