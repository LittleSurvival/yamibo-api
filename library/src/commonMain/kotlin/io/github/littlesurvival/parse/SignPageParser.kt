package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.github.littlesurvival.Parser
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.SignActionResult
import io.github.littlesurvival.dto.page.SignActionStatus
import io.github.littlesurvival.dto.page.SignCalendarDay
import io.github.littlesurvival.dto.page.SignInfoSection
import io.github.littlesurvival.dto.page.SignPage
import io.github.littlesurvival.dto.page.SignRepairOption
import io.github.littlesurvival.parse.util.ParseUtils

class SignPageParser : Parser<SignPage> {

    override suspend fun parse(html: String): ParseResult<SignPage> {
        return try {
            if (isCloudflarePage(html)) return ParseResult.Failure("Cloudflare challenge page")

            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val currentDateText = doc.select(".hui-wrap .hui-content span.y").firstOrNull()?.text()?.trim()
            val monthLabel = doc.select("#tablehead th").firstOrNull()?.ownText()?.trim()?.ifBlank { null }
            val sections = parseInfoSections(doc)
            val myActivity = findSectionItems(sections, "签到记录", "簽到記錄", "我的签到", "我的簽到", "打卡记录", "打卡記錄")
            val statistics = findSectionItems(sections, "签到统计", "簽到統計", "统计", "統計")
            val currentDateKey = currentDateText?.let(::extractDateKey)
            val lastSignDateKey = myActivity.firstNotNullOfOrNull(::extractDateKey)
            val calendarDays = parseCalendarDays(doc)
            val hasSignedToday = calendarDays.firstOrNull { it.isToday }?.isSigned == true ||
                (currentDateKey != null && currentDateKey == lastSignDateKey)

            ParseResult.Success(
                SignPage(
                    currentDateText = currentDateText,
                    monthLabel = monthLabel,
                    notice = parseNotice(doc),
                    calendarDays = calendarDays,
                    repairOptions = parseRepairOptions(doc),
                    myActivity = myActivity,
                    statistics = statistics,
                    extraSections = sections,
                    signActionUrl = doc.selectFirst(".signbtn a.btna")
                        ?.attr("href")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let(YamiboRoute.Domain::toFullLink),
                    repairActionPrefix = parseRepairActionPrefix(doc),
                    hasSignedToday = hasSignedToday,
                    lastSignDateKey = lastSignDateKey,
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse sign page", e)
        }
    }

    fun parseActionResult(html: String): ParseResult<SignActionResult> {
        return try {
            if (isCloudflarePage(html)) return ParseResult.Failure("Cloudflare challenge page")

            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val message = doc.selectFirst(".jump_c p, #messagetext p")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
                ?: doc.body().text().trim().ifBlank { null }
                ?: return ParseResult.Failure("Sign action result message not found")

            val status = when {
                message.contains("已经打过卡") ||
                    message.contains("已打过卡") ||
                    message.contains("已签到") ||
                    message.contains("已簽到") -> SignActionStatus.AlreadySigned
                message.contains("补签") || message.contains("補簽") -> SignActionStatus.RepairSuccess
                else -> SignActionStatus.Success
            }

            ParseResult.Success(SignActionResult(status = status, message = message))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse sign action result", e)
        }
    }

    private fun parseCalendarDays(doc: Document): List<SignCalendarDay> {
        return doc.select("#tablebody .day").mapNotNull { element ->
            val day = element.text().trim().toIntOrNull() ?: return@mapNotNull null
            val classes = element.classNames()
            SignCalendarDay(
                day = day,
                isSigned = classes.contains("on"),
                isToday = classes.contains("today"),
            )
        }
    }

    private fun parseRepairOptions(doc: Document): List<SignRepairOption> {
        return doc.select("#repairday option").mapNotNull { option ->
            val value = option.attr("value").trim()
            val label = option.text().trim()
            if (value.isBlank() || label.isBlank()) null else SignRepairOption(value, label)
        }
    }

    private fun parseInfoSections(doc: Document): List<SignInfoSection> {
        return doc.select(".hui-common-title-txt").mapNotNull { titleEl ->
            val title = titleEl.text().trim().ifBlank { return@mapNotNull null }
            val items = titleEl.parent()?.nextElementSibling()
                ?.select("li .hui-list-text, li")
                ?.mapNotNull { it.text().trim().ifBlank { null } }
                ?.distinct()
                .orEmpty()
            if (items.isEmpty()) null else SignInfoSection(title = title, items = items)
        }
    }

    private fun parseNotice(doc: Document): String? {
        val title = doc.select(".hui-common-title-txt").firstOrNull { titleEl ->
            val text = titleEl.text()
            text.contains("说明") || text.contains("說明") || text.contains("规则") || text.contains("規則")
        } ?: return null
        return title.parent()?.nextElementSibling()?.text()?.trim()?.ifBlank { null }
    }

    private fun findSectionItems(sections: List<SignInfoSection>, vararg keywords: String): List<String> {
        return sections.firstOrNull { section ->
            keywords.any { keyword -> section.title.contains(keyword) }
        }?.items.orEmpty()
    }

    private fun parseRepairActionPrefix(doc: Document): String? {
        val onclick = doc.selectFirst(".repairbtn")?.attr("onclick") ?: return null
        val repairToken = Regex("""repair=([^&'"]+)""").find(onclick)?.groupValues?.getOrNull(1) ?: return null
        return YamiboRoute.Domain.toFullLink("plugin.php?id=zqlj_sign&repair=$repairToken&repairday=")
    }

    private fun extractDateKey(text: String): String? {
        val match = DATE_RE.find(text) ?: return null
        val year = match.groupValues[1]
        val month = match.groupValues[2].padStart(2, '0')
        val day = match.groupValues[3].padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun isCloudflarePage(html: String): Boolean {
        val body = html.lowercase()
        return body.contains("cloudflare") ||
            body.contains("cf-chl") ||
            body.contains("just a moment") ||
            body.contains("verify you are human")
    }

    private companion object {
        val DATE_RE = Regex("""(\d{4})[^\d]+(\d{1,2})[^\d]+(\d{1,2})""")
    }
}
