package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.RateResultPopoutPage
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class RateResultPopoutPageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseRateResultPopoutPage(): Unit = runBlocking {
        val html = loadAsset("rate/rate_result_popout_page.html")
        val result = RateResultPopoutPageParser().parse(html)

        val page = assertIs<ParseResult.Success<RateResultPopoutPage>>(result).value
        assertEquals(140, page.totalScore)
        assertEquals(16, page.rates.size)
        assertEquals(2, page.rates[0].score)
        assertEquals("hyk6388", page.rates[0].userName)
        assertEquals("2026-6-20 17:27", page.rates[0].timeInfo.text)
        assertEquals("精品文章", page.rates[0].reason)
        assertEquals(10, page.rates[1].score)
        assertNull(page.rates[1].reason)
        assertEquals(10, page.rates[3].score)
        assertEquals("太强了", page.rates[3].reason)
        assertEquals(11, page.rates[4].score)
        assertEquals("精品文章", page.rates[4].reason)
        assertEquals(5, page.rates.last().score)
        assertNull(page.rates.last().reason)
    }

    @Test
    fun parseRateResultPopoutFailureMessage(): Unit = runBlocking {
        val html = """<?xml version="1.0" encoding="utf-8"?>
            |<root><![CDATA[<div class="jump_c"><p>抱歉，您没有权限查看</p></div>]]></root>
        """.trimMargin()
        val result = RateResultPopoutPageParser().parse(html)

        val failure = assertIs<ParseResult.Failure>(result)
        assertEquals("抱歉，您没有权限查看", failure.reason)
    }
}
