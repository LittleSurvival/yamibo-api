package io.github.littlesurvival.util

import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals

class PostResponseUtilTest {
    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun main() {
        val postResponses = (1..4).map { loadAsset("post_response/post_response$it.html") }
        postResponses.forEach {
            val result = PostResponseUtils.parseMessageText(it)
            println(result)
        }
    }

    @Test
    fun parseFavoriteId() {
        val postResponse = loadAsset("post_response/post_response7.html")

        assertEquals("信息收藏成功", PostResponseUtils.parseMessageText(postResponse))
        assertEquals(FavoriteId(2675784), PostResponseUtils.parseFavoriteId(postResponse))
    }
}
