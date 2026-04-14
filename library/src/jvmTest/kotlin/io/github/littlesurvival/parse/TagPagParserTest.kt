package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.AttachmentType
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class TagPagParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun parseTagPageExample1() = runBlocking {
        val html = loadAsset("tags/tag_page_example1.html")
        val result = TagPagParser().parse(html)

        val success = assertIs<ParseResult.Success<TagPage>>(result)
        val page = success.value

        println("=== TagPage ===")
        println("Tag Title: ${page.tagName}")
        println("Threads (${page.threadSummaries.size}):")
        page.threadSummaries.forEach { t ->
            println("  [tid=${t.tid}] ${t.title}")
            println("    fid=${t.fid}, attachment=${t.attachmentType}, author=${t.author}")
            println("    replies=${t.replyCount}, views=${t.viewCount}, lastUpdate=${t.lastUpdate?.text}")
        }
        println("PageNav: ${page.pageNav}")

        // Should have 20 thread summaries
        assertEquals(20, page.threadSummaries.size)

        // --- First thread ---
        val first = page.threadSummaries[0]
        assertEquals(ThreadId(552542), first.tid)
        assertTrue(first.title.contains("把我弄得乱七八糟吧"))
        assertEquals(ForumId(30), first.fid)
        assertEquals(AttachmentType.Image, first.attachmentType)
        assertEquals(UserId(360078), first.author?.uid)
        assertEquals("magtine", first.author?.name)
        assertEquals(42, first.replyCount)
        assertEquals(6678, first.viewCount)

        // --- Second thread ---
        val second = page.threadSummaries[1]
        assertEquals(ThreadId(552877), second.tid)
        assertTrue(second.title.contains("不一样的连理 47"))
        assertEquals(AttachmentType.Image, second.attachmentType)

        // --- A thread with "附件" type (thread-553121, index 4) ---
        val attachThread = page.threadSummaries[4]
        assertEquals(ThreadId(553121), attachThread.tid)
        assertEquals(AttachmentType.Other, attachThread.attachmentType)

        // --- No pagination on this example ---
        assertNull(page.pageNav)
    }
}
