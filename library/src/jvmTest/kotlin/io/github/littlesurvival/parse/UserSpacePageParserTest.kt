package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.UserSpaceBlogPage
import io.github.littlesurvival.dto.page.UserSpaceThreadPage
import io.github.littlesurvival.dto.page.UserSpaceThreadReplyPage
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class UserSpacePageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseUserSpaceThreads(): Unit = runBlocking {
        val html = loadAsset("user_space/ta的主題/主題.html")
        val result = UserSpaceThreadPageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpaceThreadPage>>(result)
        val page = success.value

        assertTrue(page.threads.isNotEmpty())
        val first = page.threads.first()
        assertEquals(ThreadId(570470), first.tid)
        assertEquals(ForumId(30), first.fid)
        assertEquals("【鰤尾みちる】貘然入梦zzZ 第4话 Kakukuroi汉化组", first.title)
        assertEquals(UserId(691344), first.author?.uid)
        assertEquals("Psychoid", first.author?.name)
        assertEquals("中文百合漫画区", first.tag)
        assertEquals(471, first.viewCount)
        assertEquals(2, first.replyCount)
        assertEquals("2026-4-28", first.lastUpdate?.text)
    }

    @Test
    fun parseUserSpaceThreadsSecondPagePrevUrl(): Unit = runBlocking {
        val html = loadAsset("user_space/ta的主題/主題page2.html")
        val result = UserSpaceThreadPageParser().parse(html)

        val page = assertIs<ParseResult.Success<UserSpaceThreadPage>>(result).value
        val pageNav = assertNotNull(page.pageNav)
        val prevUrl = assertNotNull(pageNav.prevUrl)
        val nextUrl = assertNotNull(pageNav.nextUrl)

        assertTrue(prevUrl.contains("page=1"))
        assertTrue(nextUrl.contains("page=3"))
    }

    @Test
    fun parseUserSpaceThreadReplies(): Unit = runBlocking {
        val html = loadAsset("user_space/ta的主題/回復.html")
        val result = UserSpaceThreadReplyPageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpaceThreadReplyPage>>(result)
        val page = success.value

        assertTrue(page.replies.isNotEmpty())
        val first = page.replies.first()
        assertEquals(ThreadId(535989), first.tId)
        assertEquals(null, first.fid)
        assertEquals("百合小说生肉安利专楼", first.title)
        assertEquals(1, first.posts.size)
        assertEquals(PostId(41518669), first.posts.first().pId)
        assertTrue(first.posts.first().quote.contains("ヤンデレ"))
        assertNotNull(page.pageNav?.nextUrl)
    }

    @Test
    fun parseUserSpaceMyBlogs(): Unit = runBlocking {
        val html = loadAsset("user_space/日誌/我or某人的日志.html")
        val result = UserSpaceBlogPageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpaceBlogPage>>(result)
        val page = success.value

        assertTrue(page.blogs.isNotEmpty())
        val first = page.blogs.first()
        assertEquals(BlogId(117358), first.bId)
        assertEquals("0228", first.title)
        assertEquals("好容易感到尴尬 感觉做什么事都好尴尬呀", first.description)
        assertEquals(UserId(631114), first.author.uid)
        assertEquals("tifei", first.author.name)
        assertEquals("2026-4-28 23:31", first.timeInfo.text)
        assertEquals(1, page.pageNav?.currentPage)
        assertEquals(13, page.pageNav?.totalPages)
    }

    @Test
    fun parseUserSpaceViewAllBlogs(): Unit = runBlocking {
        val html = loadAsset("user_space/日誌/隨便看看.html")
        val result = UserSpaceBlogPageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpaceBlogPage>>(result)
        val page = success.value

        assertTrue(page.blogs.isNotEmpty())
        assertEquals(BlogId(117358), page.blogs.first().bId)
        assertEquals(UserId(631114), page.blogs.first().author.uid)
        assertEquals(6402, page.pageNav?.totalPages)
    }
}
