package io.github.littlesurvival.parse.util

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseUtilsTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseJumpCMessageFromHttpErrorBody() {
        val bodyPreview = loadAsset("threads/404.html")

        assertEquals(
            "抱歉，指定的主题不存在或已被删除或正在被审核",
            ParseUtils.parseJumpCMessage(bodyPreview)
        )
    }

    @Test
    fun parseJumpCMessageReturnsNullWithoutPrompt() {
        assertNull(ParseUtils.parseJumpCMessage(null))
        assertNull(ParseUtils.parseJumpCMessage("<html><body>Not a prompt</body></html>"))
    }
}
