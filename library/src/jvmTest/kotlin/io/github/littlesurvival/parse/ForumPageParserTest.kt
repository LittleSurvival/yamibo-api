package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.page.PinnedItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ForumPageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun parseLightNovelArea() = runBlocking {
        val html = loadAsset("lightnovelarea.html")
        val result = ForumPageParser().parse(html)

        val success = assertIs<ParseResult.Success<ForumPage>>(result)
        val page = success.value
        println("=== ForumPage: ${page.forum} ===")
        println("Pinned items (${page.pinnedItems.size}):")
        page.pinnedItems.forEach { println("  $it") }
        println("Sub forums (${page.subForums.size}):")
        page.subForums.forEach { println("  $it") }
        println("Threads (${page.threads.size}):")
        page.threads.forEach { println("  $it") }
        println()

        // Forum info
        assertEquals(ForumId(55), page.forum.fid)
        assertEquals("轻小说/译文区", page.forum.name)

        // Pinned items: 1 announcement + pinned threads
        assertTrue(page.pinnedItems.isNotEmpty())
        val announcement = page.pinnedItems.firstOrNull { it is PinnedItem.Announcement }
        assertNotNull(announcement)
        assertIs<PinnedItem.Announcement>(announcement)
        assertTrue(announcement.title.contains("欢迎光临"))

        val pinnedThreads = page.pinnedItems.filterIsInstance<PinnedItem.Thread>()
        assertTrue(pinnedThreads.isNotEmpty())
        // First pinned thread: tid=533721
        assertEquals(ThreadId(533721), pinnedThreads[0].tid)

        // Thread list
        assertTrue(page.threads.size >= 14)

        // First thread
        val first = page.threads[0]
        assertEquals(ThreadId(535989), first.tid)
        assertEquals(UserId(165700), first.author?.uid)
        assertEquals("hongyuny", first.author?.name)
        assertNotNull(first.description)
        assertEquals(85773, first.viewCount)
        assertEquals(211, first.replyCount)
        assertEquals("公告", first.tag)
        assertEquals("2023-5-29 22:41", first.lastUpdateText)
    }

    @Test
    fun parseWenxueArea() = runBlocking {
        val html = loadAsset("wenxuearea.html")
        val result = ForumPageParser().parse(html)

        val success = assertIs<ParseResult.Success<ForumPage>>(result)
        val page = success.value
        println("=== ForumPage: ${page.forum} ===")
        println("Pinned items (${page.pinnedItems.size}):")
        page.pinnedItems.forEach { println("  $it") }
        println("Sub forums (${page.subForums.size}):")
        page.subForums.forEach { println("  $it") }
        println("Threads (${page.threads.size}):")
        page.threads.forEach { println("  $it") }
        println()

        // Forum info
        assertEquals(ForumId(49), page.forum.fid)
        assertEquals("文學區", page.forum.name)

        // Sub forums
        assertEquals(2, page.subForums.size)
        assertEquals(ForumId(55), page.subForums[0].fid)
        assertEquals("轻小说/译文区", page.subForums[0].name)
        assertEquals(ForumId(60), page.subForums[1].fid)
        assertEquals("TXT小说区", page.subForums[1].name)

        // Pinned items
        assertTrue(page.pinnedItems.isNotEmpty())

        // Thread list
        assertTrue(page.threads.isNotEmpty())

        // First thread
        val first = page.threads[0]
        assertEquals(ThreadId(564689), first.tid)
        assertEquals(UserId(209815), first.author?.uid)
        assertEquals("21058700", first.author?.name)
    }
}
