package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ThreadPageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun parseThreadExample1() = runBlocking {
        val html = loadAsset("threadexample1.html")
        val result = ThreadPageParser().parse(html)

        val success = assertIs<ParseResult.Success<ThreadPage>>(result)
        val page = success.value
        println("=== ThreadPage ===")
        println("Thread: ${page.thread}")
        println("PageNav: ${page.pageNav}")
        println("Posts (${page.posts.size}):")
        page.posts.forEach { post ->
            println("  Post #${post.floor} (pid=${post.pid}):")
            println("    Author: ${post.author}")
            println("    Time: ${post.timeText}")
            if (post.editedText != null) println("    Edited: ${post.editedText}")
            println("    Content: ${post.contentHtml.take(120)}...")
            if (post.images.isNotEmpty()) println("    Images: ${post.images}")
        }
        println()

        // Thread info
        assertEquals(ThreadId(566803), page.thread.tid)
        assertTrue(page.thread.title.contains("与女神的Q"))
        assertEquals("[轻小说]", page.thread.categoryTag)

        // Forum
        assertEquals(ForumId(55), page.thread.forum.fid)
        assertEquals("轻小说/译文区", page.thread.forum.name)

        // Posts: should have multiple
        assertTrue(page.posts.size >= 5)

        // First post
        val firstPost = page.posts[0]
        assertEquals(PostId(41458174), firstPost.pid)
        assertEquals(1, firstPost.floor)
        assertEquals(UserId(657969), firstPost.author.uid)
        assertEquals("11111111zlk", firstPost.author.name)
        assertNotNull(firstPost.author.avatarUrl)
        assertEquals("2026-1-29 21:07", firstPost.timeText)
        assertNotNull(firstPost.editedText)
        assertTrue(firstPost.editedText!!.contains("编辑"))
        assertTrue(firstPost.contentHtml.contains("接坑"))

        // Second post
        val secondPost = page.posts[1]
        assertEquals(PostId(41458180), secondPost.pid)
        assertEquals(2, secondPost.floor)
        assertEquals("2026-1-29 21:16", secondPost.timeText)
        assertTrue(secondPost.contentHtml.contains("第十一话"))

        // Third post
        val thirdPost = page.posts[2]
        assertEquals(PostId(41458185), thirdPost.pid)
        assertEquals(3, thirdPost.floor)
    }

    @Test
    fun parseThreadExample4_commentsRatesTotalRepliesReplyUrl() = runBlocking {
        val html = loadAsset("threadexample4.html")
        val result = ThreadPageParser().parse(html)

        val success = assertIs<ParseResult.Success<ThreadPage>>(result)
        val page = success.value
        println("=== ThreadPage (example4) ===")
        println("Thread: ${page.thread}")
        println("TotalReplies: ${page.thread.totalReplies}")
        println("ReplyUrl: ${page.thread.replyUrl}")
        println("Posts (${page.posts.size}):")
        page.posts.forEach { post ->
            println("  Post #${post.floor} (pid=${post.pid}):")
            println("    Author: ${post.author}")
            println("    Time: ${post.timeText}")
            println("    Comments (${post.comments.size}): ${post.comments}")
            println("    Rates (${post.rates.size}): ${post.rates}")
        }
        println()

        // Thread info
        assertEquals(ThreadId(556787), page.thread.tid)
        assertTrue(page.thread.title.contains("室友好像是少女遊戲的女主角"))
        assertEquals("[轻小说]", page.thread.categoryTag)
        assertEquals(ForumId(55), page.thread.forum.fid)

        // Total replies and views (page 1 only)
        assertEquals(1383, page.thread.totalReplies)
        assertEquals(69614, page.thread.totalViews)

        // Reply URL
        assertNotNull(page.thread.replyUrl)
        assertTrue(page.thread.replyUrl!!.contains("action=reply"))
        assertTrue(page.thread.replyUrl!!.contains("tid=556787"))

        // First post (floor 1) - should have comments and rates
        val firstPost = page.posts[0]
        assertEquals(PostId(41239943), firstPost.pid)
        assertEquals(1, firstPost.floor)

        // Comments on first post
        assertTrue(firstPost.comments.isNotEmpty(), "First post should have comments")
        assertEquals(2, firstPost.comments.size)
        assertEquals("inchy", firstPost.comments[0].user.name)
        assertEquals(UserId(680505), firstPost.comments[0].user.uid)
        assertTrue(firstPost.comments[0].message.contains("译者很傲娇"))
        assertEquals("ccwb24", firstPost.comments[1].user.name)
        assertTrue(firstPost.comments[1].message.contains("譯者很高冷"))

        // Rates on first post
        assertTrue(firstPost.rates.isNotEmpty(), "First post should have rates")
        assertEquals("rluojiu", firstPost.rates[0].userName)
        assertEquals("好萌好萌好萌", firstPost.rates[0].reason)
        // Check a rate without reason
        val noReasonRate = firstPost.rates.find { it.userName == "3379510073" }
        assertNotNull(noReasonRate)
        assertEquals("", noReasonRate.reason)

        // Second post (floor 2) - has rates but no comments
        val secondPost = page.posts[1]
        assertEquals(PostId(41240199), secondPost.pid)
        assertEquals(2, secondPost.floor)
        assertTrue(secondPost.comments.isEmpty(), "Second post should have no comments")
        assertTrue(secondPost.rates.isNotEmpty(), "Second post should have rates")

        // Third post (floor 3) - should have no comments and no rates
        val thirdPost = page.posts[2]
        assertEquals(PostId(41240351), thirdPost.pid)
        assertEquals(3, thirdPost.floor)
        assertTrue(thirdPost.comments.isEmpty(), "Third post should have no comments")
        assertTrue(thirdPost.rates.isEmpty(), "Third post should have no rates")
    }
}
