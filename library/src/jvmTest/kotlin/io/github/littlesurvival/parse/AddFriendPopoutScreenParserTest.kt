package io.github.littlesurvival.parse

import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.AddFriendPopoutScreen
import io.github.littlesurvival.dto.value.UserId
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class AddFriendPopoutScreenParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseAddFriendPopoutScreen(): Unit = runBlocking {
        val html = loadAsset("user_space/新增好友/add_friend_popout.html")
        val result = AddFriendPopoutScreenParser().parse(html)

        val page = assertIs<ParseResult.Success<AddFriendPopoutScreen>>(result).value
        assertEquals(UserId(630885), page.user.uid)
        assertEquals("fluchtcn", page.user.name)
        assertEquals(
            "https://bbs.yamibo.com/uc_server/data/avatar/000/63/08/85_avatar_small.jpg",
            page.user.avatarUrl
        )
        assertEquals(8, page.availableOption.size)
        assertEquals(0, page.availableOption.first().id)
        assertEquals("其他", page.availableOption.first().reason)
        assertEquals(1, page.availableOption[1].id)
        assertEquals("通过本站认识", page.availableOption[1].reason)
    }

    @Test
    fun parseAddFriendPostResponseMessage(): Unit = runBlocking {
        val html = loadAsset("user_space/新增好友/send_success.html")
        val message = io.github.littlesurvival.fetch.post.util.PostResponseUtils.parseMessageText(html)

        assertEquals("好友请求已发送，请等待对方验证", message)
        assertTrue(io.github.littlesurvival.fetch.post.util.PostResponseUtils.isSuccess(html))
    }
}
