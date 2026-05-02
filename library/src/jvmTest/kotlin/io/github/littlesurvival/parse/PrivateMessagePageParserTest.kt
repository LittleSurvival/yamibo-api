package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.PrivateMessagePage
import io.github.littlesurvival.dto.page.PrivateMessageType
import io.github.littlesurvival.dto.value.PrivateMessageId
import io.github.littlesurvival.dto.value.UserId
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PrivateMessagePageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parsePrivateMessageFirstPage(): Unit = runBlocking {
        val html = loadAsset("private_message/pm_page1.html")
        val result = PrivateMessagePageParser().parse(html)

        val page = assertIs<ParseResult.Success<PrivateMessagePage>>(result).value
        assertEquals(UserId(723881), page.toUser)
        assertEquals(PrivateMessageId(747702), page.pmId)
        assertEquals("正在与littlesurvival聊天中……", page.title)
        assertEquals(1, page.messages.size)

        val message = page.messages.first()
        assertEquals(PrivateMessageType.Friend, message.messageType)
        assertEquals(UserId(723881), message.user.uid)
        assertEquals("littlesurvival", message.user.name)
        assertTrue(message.contentHtml.contains("<strong>test</strong>"))
        assertTrue(message.contentHtml.contains("blockquote"))
        assertEquals("2026-4-29 04:36", message.timeInfo.text)

        val pageNav = assertNotNull(page.pageNav)
        assertNull(pageNav.prevUrl)
        assertEquals(2, pageNav.nextPageIndex)
        assertEquals(1, pageNav.currentPage)
    }

    @Test
    fun parsePrivateMessageLatestPage(): Unit = runBlocking {
        val html = loadAsset("private_message/pm_page2.html")
        val result = PrivateMessagePageParser().parse(html)

        val page = assertIs<ParseResult.Success<PrivateMessagePage>>(result).value
        assertEquals(UserId(723881), page.toUser)
        assertEquals(PrivateMessageId(747716), page.pmId)
        assertEquals(5, page.messages.size)

        val selfMessage = page.messages.first {
            it.messageType == PrivateMessageType.Self && it.contentHtml.contains("b_test")
        }
        assertEquals(UserId(656626), selfMessage.user.uid)
        assertTrue(selfMessage.contentHtml.contains("<strong>b_test</strong>"))
        assertTrue(selfMessage.contentHtml.contains("blockcode"))
        assertTrue(selfMessage.contentHtml.contains("coolmonkey"))

        val pageNav = assertNotNull(page.pageNav)
        assertEquals(1, pageNav.prevPageIndex)
        assertEquals(2, pageNav.currentPage)
        assertNull(pageNav.nextUrl)
    }
}
