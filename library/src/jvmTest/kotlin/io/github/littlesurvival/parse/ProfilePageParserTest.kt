package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.value.UserId
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class ProfilePageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseAvatarBackgroundUrl(): Unit = runBlocking {
        val html = loadAsset("profilepage.html")
        val result = ProfilePageParser().parse(html)

        val page = assertIs<ParseResult.Success<ProfilePage>>(result).value
        assertEquals(UserId(656626), page.uid)
        assertEquals("https://bbs.yamibo.com/uc_server/data/avatar/000/65/66/26_avatar_big.jpg", page.avatarBackgroundUrl)
    }
}
