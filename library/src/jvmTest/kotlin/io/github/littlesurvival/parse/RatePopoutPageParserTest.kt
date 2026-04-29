package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.RatePopoutPage
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class RatePopoutPageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseRatePopoutScreen(): Unit = runBlocking {
        val html = loadAsset("rate/rate_popout_screen.html")
        val result = RatePopoutPageParser().parse(html)

        val page = assertIs<ParseResult.Success<RatePopoutPage>>(result).value
        assertEquals(listOf(0, 5, 4, 3, 2, 1), page.availableScores)
        assertEquals(
            listOf("你太可爱", "好萌好萌好萌", "我很赞同", "精品文章", "原创内容"),
            page.defaultReasons
        )
    }

    @Test
    fun parsePostResponseWhenRatePopoutHasNoFormData(): Unit = runBlocking {
        val html = loadAsset("post_response/post_response1.html")
        val result = RatePopoutPageParser().parse(html)

        val failure = assertIs<ParseResult.Failure>(result)
        assertEquals("抱歉，24 小时评分数超过限制", failure.reason)
    }
}
