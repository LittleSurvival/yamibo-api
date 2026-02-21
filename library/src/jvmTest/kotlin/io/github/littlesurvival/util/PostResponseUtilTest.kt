package io.github.littlesurvival.util

import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import kotlin.test.Test

class PostResponseUtilTest {
    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader()
            .readText()
    }

    @Test
    fun main() {
        val postResponses = (1..4).map { loadAsset("post_response$it.html") }
        postResponses.forEach {
            val result = PostResponseUtils.parseMessageText(it)
            println(result)
        }
    }
}