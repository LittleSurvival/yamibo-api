package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.SignActionResult
import io.github.littlesurvival.dto.page.SignActionStatus
import io.github.littlesurvival.dto.page.SignPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SignPageParserTest {

    @Test
    fun parseSignPage(): Unit = runBlocking {
        val result = SignPageParser().parse(SIGN_PAGE_HTML)

        val page = assertIs<ParseResult.Success<SignPage>>(result).value
        assertEquals("2026-05-10", page.currentDateText)
        assertEquals("2026年5月", page.monthLabel)
        assertEquals("每天只能签到一次。", page.notice)
        assertEquals(3, page.calendarDays.size)
        assertTrue(page.calendarDays.first { it.day == 10 }.isToday)
        assertTrue(page.calendarDays.first { it.day == 10 }.isSigned)
        assertEquals("2026-05-10", page.lastSignDateKey)
        assertTrue(page.hasSignedToday)

        assertEquals(1, page.repairOptions.size)
        assertEquals("2026-05-01", page.repairOptions.first().value)
        assertEquals("5月1日", page.repairOptions.first().label)
        assertEquals("https://bbs.yamibo.com/plugin.php?id=zqlj_sign&sign=abc", page.signActionUrl)
        assertEquals(
            "https://bbs.yamibo.com/plugin.php?id=zqlj_sign&repair=repair_token&repairday=",
            page.repairActionPrefix
        )
        assertEquals(listOf("2026-05-10 签到成功"), page.myActivity)
        assertEquals(listOf("累计签到 12 天"), page.statistics)
        assertEquals(3, page.extraSections.size)
    }

    @Test
    fun parseSignActionResult(): Unit = runBlocking {
        val result = SignPageParser().parseActionResult(
            """<html><body><div class="jump_c"><p>签到成功，获得奖励。</p></div></body></html>"""
        )

        val action = assertIs<ParseResult.Success<SignActionResult>>(result).value
        assertEquals(SignActionStatus.Success, action.status)
        assertEquals("签到成功，获得奖励。", action.message)
    }

    @Test
    fun parseAlreadySignedActionResult(): Unit = runBlocking {
        val result = SignPageParser().parseActionResult(
            """<html><body><div class="jump_c"><p>您今天已经打过卡了。</p></div></body></html>"""
        )

        val action = assertIs<ParseResult.Success<SignActionResult>>(result).value
        assertEquals(SignActionStatus.AlreadySigned, action.status)
    }

    @Test
    fun parseRepairActionResult(): Unit = runBlocking {
        val result = SignPageParser().parseActionResult(
            """<html><body><div class="jump_c"><p>补签成功。</p></div></body></html>"""
        )

        val action = assertIs<ParseResult.Success<SignActionResult>>(result).value
        assertEquals(SignActionStatus.RepairSuccess, action.status)
    }

    @Test
    fun rejectCloudflarePage(): Unit = runBlocking {
        val result = SignPageParser().parse("<html><title>Just a moment...</title><body>cf-chl</body></html>")

        val failure = assertIs<ParseResult.Failure>(result)
        assertTrue(failure.reason.contains("Cloudflare"))
    }

    private companion object {
        val SIGN_PAGE_HTML = """
            <html>
              <body>
                <div class="hui-wrap">
                  <div class="hui-content"><span class="y">2026-05-10</span></div>
                </div>
                <table>
                  <thead id="tablehead"><tr><th>2026年5月</th></tr></thead>
                  <tbody id="tablebody">
                    <tr>
                      <td><span class="day on">9</span></td>
                      <td><span class="day today on">10</span></td>
                      <td><span class="day">11</span></td>
                    </tr>
                  </tbody>
                </table>
                <div><span class="hui-common-title-txt">签到说明</span></div>
                <ul><li><div class="hui-list-text">每天只能签到一次。</div></li></ul>
                <div><span class="hui-common-title-txt">我的签到记录</span></div>
                <ul><li><div class="hui-list-text">2026-05-10 签到成功</div></li></ul>
                <div><span class="hui-common-title-txt">签到统计</span></div>
                <ul><li><div class="hui-list-text">累计签到 12 天</div></li></ul>
                <div class="signbtn"><a class="btna" href="plugin.php?id=zqlj_sign&amp;sign=abc">签到</a></div>
                <button class="repairbtn" onclick="location.href='plugin.php?id=zqlj_sign&amp;repair=repair_token&amp;repairday='">补签</button>
                <select id="repairday">
                  <option value="">请选择</option>
                  <option value="2026-05-01">5月1日</option>
                </select>
              </body>
            </html>
        """.trimIndent()
    }
}
