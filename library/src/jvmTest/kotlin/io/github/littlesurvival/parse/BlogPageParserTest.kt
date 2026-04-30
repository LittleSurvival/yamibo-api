package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.BlogPage
import io.github.littlesurvival.dto.value.BlogCommentId
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.UserId
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class BlogPageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseBlogPage(): Unit = runBlocking {
        val html = loadAsset("blog/blogpage1.html")
        val result = BlogPageParser().parse(html)

        val page = assertIs<ParseResult.Success<BlogPage>>(result).value
        assertEquals(BlogId(117056), page.blogInfo.blogId)
        assertEquals("圣みかみ的圣言——读者是上帝还是异端？", page.blogInfo.title)
        assertEquals(411, page.blogInfo.totalViews)
        assertEquals(48, page.blogInfo.totalReplies)
        assertNotNull(page.blogInfo.collectUrl)
        assertNotNull(page.blogInfo.shareUrl)
        assertNotNull(page.blogInfo.inviteUrl)

        assertNull(page.rootBlog.bcId)
        assertEquals(UserId(534724), page.rootBlog.author.uid)
        assertEquals("hjfun", page.rootBlog.author.name)
        assertEquals("2026-2-12 17:00", page.rootBlog.timeInfo.text)
        assertTrue(page.rootBlog.contentHtml.contains("首先，我原本是打算发帖的"))

        val firstComment = page.blogComments.first()
        assertEquals(BlogCommentId(645992), firstComment.bcId)
        assertEquals(UserId(594846), firstComment.author.uid)
        assertEquals("key913", firstComment.author.name)
        assertEquals("2026-2-12 18:26", firstComment.timeInfo.text)
        assertNotNull(firstComment.replyUrl)
        assertTrue(firstComment.contentHtml.contains("肯定是读者的问题啦"))
        assertEquals(1, page.pageNav?.currentPage)
        assertEquals(3, page.pageNav?.totalPages)
    }

    @Test
    fun parseBlogPageSecondPageKeepsRootBlog(): Unit = runBlocking {
        val page1 = assertIs<ParseResult.Success<BlogPage>>(
            BlogPageParser().parse(loadAsset("blog/blogpage1.html"))
        ).value
        val page2 = assertIs<ParseResult.Success<BlogPage>>(
            BlogPageParser().parse(loadAsset("blog/blogpage2.html"))
        ).value

        assertEquals(page1.blogInfo, page2.blogInfo)
        assertEquals(page1.rootBlog, page2.rootBlog)
        assertEquals(BlogCommentId(646016), page2.blogComments.first().bcId)
        assertTrue(page2.blogComments.first().contentHtml.contains("好像确实是这样"))
        assertEquals(2, page2.pageNav?.currentPage)
        assertEquals(3, page2.pageNav?.totalPages)
    }
}
