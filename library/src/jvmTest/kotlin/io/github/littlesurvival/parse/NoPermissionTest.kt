package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class NoPermissionTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun threadPageParserDetectsNoPermission() = runBlocking {
        val html = loadAsset("no_permission/鮮血王女.html")
        val result = ThreadPageParser().parse(html)
        println("ThreadPageParser + no permission page => $result")
        assertIs<ParseResult.NoPermission>(result)
        Unit
    }

    @Test
    fun forumPageParserDetectsNoPermission() = runBlocking {
        val html = loadAsset("no_permission/鮮血王女.html")
        val result = ForumPageParser().parse(html)
        println("ForumPageParser + no permission page => $result")
        assertIs<ParseResult.NoPermission>(result)
        Unit
    }

    @Test
    fun homePageParserDetectsNoPermission() = runBlocking {
        val html = loadAsset("no_permission/鮮血王女.html")
        val result = HomePageParser().parse(html)
        println("HomePageParser + no permission page => $result")
        assertIs<ParseResult.NoPermission>(result)
        Unit
    }
}
