package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class NotLoggedInTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun homePageParserDetectsNotLoggedInMobile() = runBlocking {
        val html = loadAsset("notloggedin_mobile.html")
        val result = HomePageParser().parse(html)
        println("HomePageParser + mobile login page => $result")
        assertIs<ParseResult.NotLoggedIn>(result)
        Unit
    }

    @Test
    fun homePageParserDetectsNotLoggedInDesktop() = runBlocking {
        val html = loadAsset("notloggedin_desktop.html")
        val result = HomePageParser().parse(html)
        println("HomePageParser + desktop login page => $result")
        assertIs<ParseResult.NotLoggedIn>(result)
        Unit
    }

    @Test
    fun forumPageParserDetectsNotLoggedInMobile() = runBlocking {
        val html = loadAsset("notloggedin_mobile.html")
        val result = ForumPageParser().parse(html)
        println("ForumPageParser + mobile login page => $result")
        assertIs<ParseResult.NotLoggedIn>(result)
        Unit
    }

    @Test
    fun forumPageParserDetectsNotLoggedInDesktop() = runBlocking {
        val html = loadAsset("notloggedin_desktop.html")
        val result = ForumPageParser().parse(html)
        println("ForumPageParser + desktop login page => $result")
        assertIs<ParseResult.NotLoggedIn>(result)
        Unit
    }

    @Test
    fun threadPageParserDetectsNotLoggedInMobile() = runBlocking {
        val html = loadAsset("notloggedin_mobile.html")
        val result = ThreadPageParser().parse(html)
        println("ThreadPageParser + mobile login page => $result")
        assertIs<ParseResult.NotLoggedIn>(result)
        Unit
    }

    @Test
    fun threadPageParserDetectsNotLoggedInDesktop() = runBlocking {
        val html = loadAsset("notloggedin_desktop.html")
        val result = ThreadPageParser().parse(html)
        println("ThreadPageParser + desktop login page => $result")
        assertIs<ParseResult.NotLoggedIn>(result)
        Unit
    }
}
