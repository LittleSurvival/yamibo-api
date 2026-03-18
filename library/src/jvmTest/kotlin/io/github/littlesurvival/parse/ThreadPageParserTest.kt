package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import toKotlinCode
import java.io.File

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
            println("    Rates (${post.rateBlock.rates.size}): ${post.rateBlock.rates}")
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
        assertTrue(page.thread.replyUrl.contains("action=reply"))
        assertTrue(page.thread.replyUrl.contains("tid=556787"))

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
        assertTrue(firstPost.rateBlock.rates.isNotEmpty(), "First post should have rates")
        assertEquals("rluojiu", firstPost.rateBlock.rates[0].userName)
        assertEquals("好萌好萌好萌", firstPost.rateBlock.rates[0].reason)
        // Check a rate without reason
        val noReasonRate = firstPost.rateBlock.rates.find { it.userName == "3379510073" }
        assertNotNull(noReasonRate)
        assertEquals("", noReasonRate.reason)
        assertEquals(21, firstPost.rateBlock.rateParticipatePeople)
        assertEquals(172, firstPost.rateBlock.rateTotalScore)

        // Second post (floor 2) - has rates but no comments
        val secondPost = page.posts[1]
        assertEquals(PostId(41240199), secondPost.pid)
        assertEquals(2, secondPost.floor)
        assertTrue(secondPost.comments.isEmpty(), "Second post should have no comments")
        assertTrue(secondPost.rateBlock.rates.isNotEmpty(), "Second post should have rates")
        assertEquals(1, secondPost.rateBlock.rateParticipatePeople)
        assertEquals(5, secondPost.rateBlock.rateTotalScore)

        // Third post (floor 3) - should have no comments and no rates
        val thirdPost = page.posts[2]
        assertEquals(PostId(41240351), thirdPost.pid)
        assertEquals(3, thirdPost.floor)
        assertTrue(thirdPost.comments.isEmpty(), "Third post should have no comments")
        assertTrue(thirdPost.rateBlock.rates.isEmpty(), "Third post should have no rates")
    }

    @Test
    fun parseThreadAttachments() = runBlocking {
        val html = loadAsset("post_response5.html")
        val result = ThreadPageParser().parse(html)

        val success = assertIs<ParseResult.Success<ThreadPage>>(result)
        val page = success.value

        // In post_response5.html, pid 41444606 has an attachment. Let's find it.
        val targetPost = page.posts.find { it.pid == PostId(41444606) }
        assertNotNull(targetPost, "Post 41444606 not found")

        assertTrue(targetPost.attachments.isNotEmpty(), "Target post should have attachments")
        val attachment = targetPost.attachments[0]
        assertEquals("chapter57 羽花（2）.txt", attachment.name)
        assertTrue(
                attachment.url.contains("mod=attachment&aid="),
                "Attachment URL should contain mod=attachment"
        )
        assertEquals("2026-1-10 21:08", attachment.timeUpload)
        assertEquals("17.93 KB", attachment.fileSize)
        assertEquals(122, attachment.downloadTimes)

        // Test the second html example for attachments
        val html2 = loadAsset("threads/義妹後輩.html")
        val result2 = ThreadPageParser().parse(html2)
        val success2 = assertIs<ParseResult.Success<ThreadPage>>(result2)
        val page2 = success2.value

        val targetPost2 = page2.posts.find { it.pid == PostId(40723956) }
        assertNotNull(targetPost2, "Post 40723956 not found")
        assertTrue(targetPost2.attachments.isNotEmpty(), "Target post should have attachments")
        assertEquals(2, targetPost2.attachments.size)

        val attach1 = targetPost2.attachments[0]
        assertEquals("直到与变成了义妹的毒舌系后辈成为真正的家人.txt", attach1.name)
        assertEquals("2025-6-11 00:17", attach1.timeUpload)
        assertEquals("281.46 KB", attach1.fileSize)
        assertEquals(4226, attach1.downloadTimes)

        val attach2 = targetPost2.attachments[1]
        assertEquals("直到与变成了义妹的毒舌系后辈成为真正的家人.epub", attach2.name)
        assertEquals("2025-6-11 00:17", attach2.timeUpload)
        assertEquals("153.7 KB", attach2.fileSize)
        assertEquals(399, attach2.downloadTimes)
    }

    @Test
    fun dumpAllThreadsAndPollsToTxt() = runBlocking {
        val parser = ThreadPageParser()
        val assetsDir = File("src/commonTest/resources/assets")
        val txtDir = File("src/commonTest/resources/txt")
        if (!txtDir.exists()) txtDir.mkdirs()

        val htmlFiles = mutableListOf<File>()
        
        // Add thread examples
        assetsDir.listFiles { _, name -> name.startsWith("threadexample") && name.endsWith(".html") }?.forEach { htmlFiles.add(it) }
        
        // Add post_response files
        assetsDir.listFiles { _, name -> name.startsWith("post_response") && name.endsWith(".html") }?.forEach { htmlFiles.add(it) }
        
        // Add files from threads dir
        File(assetsDir, "threads").listFiles { _, name -> name.endsWith(".html") }?.forEach { htmlFiles.add(it) }

        // Add files from special_threads dir
        File(assetsDir, "special_threads").listFiles { _, name -> name.endsWith(".html") }?.forEach { htmlFiles.add(it) }
        
        // Add files from poll dir
        val pollDir = File(assetsDir, "poll")
        if (pollDir.exists()) {
            pollDir.listFiles { _, name -> name.endsWith(".html") && !name.contains("forum") }?.forEach { htmlFiles.add(it) }
        }

        // Add files from tags dir
        val tagsDir = File(assetsDir, "tags")
        if (tagsDir.exists()) {
            tagsDir.listFiles { _, name -> name.endsWith(".html") }?.forEach { htmlFiles.add(it) }
        }

        // Add files from manga dir
        val mangaDir = File(assetsDir, "manga")
        if (mangaDir.exists()) {
            mangaDir.listFiles { _, name -> name.endsWith(".html") }?.forEach { htmlFiles.add(it) }
        }

        for (file in htmlFiles) {
            val html = file.readText()
            val result = parser.parse(html)
            if (result is ParseResult.Success) {
                println("Dumping ${file.name} to txt...")
                File(txtDir, file.name).writeText(toKotlinCode(result.value, indentSize = 4))
            } else {
                println("Failed to parse ${file.name}: $result")
            }
        }
    }

    @Test
    fun parseMangaTestImages() = runBlocking {
        val html = loadAsset("manga/manga_test1.html")
        val result = ThreadPageParser().parse(html)
        val success = assertIs<ParseResult.Success<ThreadPage>>(result)
        val page = success.value

        val firstPost = page.posts[0]
        assertEquals(1, firstPost.floor)

        assertTrue(firstPost.images.isNotEmpty(), "First post should have manga images")
        val firstImage = firstPost.images[0]
        assertTrue(firstImage.url.contains("001843kg7hihl9dhepbrlb.png"), "Image URL should be parsed")
        
        // Ensure contentHtml contains img_one
        assertTrue(firstPost.contentHtml.contains("img_one"), "Content HTML should contain img_one")
    }

    @Test
    fun parseThreadTagsFromMobileAsset() = runBlocking {
        val html = loadAsset("tags/女友亦女友6_mobile.html")
        val result = ThreadPageParser().parse(html)

        val success = assertIs<ParseResult.Success<ThreadPage>>(result)
        val page = success.value

        // In 女友亦女友6_mobile.html, first post (floor 1) should have tags
        val firstPost = page.posts[0]
        assertEquals(1, firstPost.floor)
        assertTrue(firstPost.tags.value.isNotEmpty(), "First post should have tags")

        // Look for the "本作目录" tag
        val catalogTag = firstPost.tags.value.find { it.name == "本作目录" }
        assertNotNull(catalogTag, "Tag '本作目录' not found")
        assertEquals(TagId(21578), catalogTag.id)
    }
}
