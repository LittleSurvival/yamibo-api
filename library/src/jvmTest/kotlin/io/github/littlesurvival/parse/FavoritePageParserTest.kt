package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.FavoritePage
import io.github.littlesurvival.dto.page.FavoriteType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class FavoritePageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun parseFavoritePage(): Unit = runBlocking {
        val html = loadAsset("favorite.html")
        val result = FavoritePageParser().parse(html)

        val success = assertIs<ParseResult.Success<FavoritePage>>(result)
        val page = success.value
        println("=== FavoritePage ===")
        println("Type: ${page.type}")
        println("Items (${page.items.size}):")
        page.items.forEach { println("  ${it.name} | ${it.url} | ${it.deleteUrl}") }
        println("PageNav: ${page.pageNav}")
        println()

        // Favorite type: the active tab is "帖子" → Thread
        assertEquals(FavoriteType.Thread, page.type)

        // Items count: 20 items in the HTML
        assertEquals(20, page.items.size)

        // First item
        val first = page.items[0]
        assertTrue(first.name.contains("孤僻的我向可爱的女孩子进行了意外的爱的告白"))
        assertEquals("forum.php?mod=viewthread&tid=555362&mobile=2", first.url)
        assertEquals(
                "home.php?mod=spacecp&ac=favorite&op=delete&favid=2469660&mobile=2",
                first.deleteUrl
        )

        // Last item
        val last = page.items.last()
        assertTrue(last.name.contains("我的朋友毫不踌躇"))
        assertEquals("forum.php?mod=viewthread&tid=565218&mobile=2", last.url)
        assertEquals(
                "home.php?mod=spacecp&ac=favorite&op=delete&favid=2454764&mobile=2",
                last.deleteUrl
        )

        // Pagination
        assertNotNull(page.pageNav)
        assertEquals(1, page.pageNav.currentPage)
        assertEquals(13, page.pageNav.totalPages)
        assertNotNull(page.pageNav.nextUrl)
    }
}
