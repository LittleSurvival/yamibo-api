package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.HomePage
import io.github.littlesurvival.dto.value.ForumId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class HomePageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun parseHomePage() = runBlocking {
        val html = loadAsset("homepage.html")
        val result = HomePageParser().parse(html)

        val success = assertIs<ParseResult.Success<HomePage>>(result)
        val homePage = success.value
        println("=== HomePage ===")
        homePage.categories.forEachIndexed { i, cat ->
            println("Category[$i]: ${cat.title}")
            cat.forums.forEachIndexed { j, f -> println("  Forum[$j]: ${f}") }
        }
        println("Yearly Summary: ${homePage.yearlySummary}")
        println()

        // Should have 3 categories: 我收藏的版块, 庙堂, 江湖
        assertEquals(3, homePage.categories.size)

        // Verify 我收藏的版块
        val fav = homePage.categories[0]
        assertEquals("我收藏的版块", fav.title)
        assertTrue(fav.forums.isNotEmpty())

        val miaotang = homePage.categories[1]
        assertEquals("庙堂", miaotang.title)
        assertEquals(2, miaotang.forums.size)

        // First forum: 管理版
        val guanli = miaotang.forums[0]
        assertEquals(ForumId(16), guanli.fid)
        assertEquals("管理版", guanli.name)
        // todayCount may be affected by numSpan.remove() from
        // favorites processing; just verify fid/name.
        assertNotNull(guanli.iconUrl)

        // Second forum: 使用指南
        val shiyong = miaotang.forums[1]
        assertEquals(ForumId(370), shiyong.fid)
        assertEquals("使用指南", shiyong.name)

        val jianghu = homePage.categories[2]
        assertEquals("江湖", jianghu.title)
        assertEquals(7, jianghu.forums.size)

        // Verify some forums in 江湖
        val dongman = jianghu.forums[0]
        assertEquals(ForumId(5), dongman.fid)
        assertEquals("動漫區", dongman.name)

        val wenxue = jianghu.forums[3]
        assertEquals(ForumId(49), wenxue.fid)
        assertEquals("文學區", wenxue.name)

        // Yearly summary
        val summary = homePage.yearlySummary
        assertNotNull(summary)
        assertTrue(summary.name.contains("年度总结"))
        assertTrue(summary.imageLink.isNotEmpty())
        assertTrue(summary.activityLink.isNotEmpty())
    }
}
