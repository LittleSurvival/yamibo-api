package io.github.littlesurvival.parse

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.NoticeType
import io.github.littlesurvival.dto.page.UserSpaceFriendPage
import io.github.littlesurvival.dto.page.UserSpaceNoticePage
import io.github.littlesurvival.dto.page.UserSpacePrivateMessagePage
import io.github.littlesurvival.dto.value.NoticeId
import io.github.littlesurvival.dto.value.UserId
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class UserSpaceFriendNotificationPageParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseMyFriends(): Unit = runBlocking {
        val html = loadAsset("user_space/我的好友/我的好友.html")
        val result = UserSpaceFriendPageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpaceFriendPage>>(result)
        val page = success.value

        assertEquals(YamiboRoute.UserSpace.FriendPageType.MyFriend, page.type)
        assertTrue(page.users.isNotEmpty())
        val first = page.users.first()
        assertEquals(UserId(646528), first.user.uid)
        assertEquals("pipeyume", first.user.name)
        assertNotNull(first.pmUrl)
        assertNotNull(first.deleteUrl)
    }

    @Test
    fun parseOnlineMembers(): Unit = runBlocking {
        val html = loadAsset("user_space/我的好友/在線成員.html")
        val result = UserSpaceFriendPageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpaceFriendPage>>(result)
        val page = success.value

        assertEquals(YamiboRoute.UserSpace.FriendPageType.OnlineMember, page.type)
        assertEquals(UserId(538843), page.users.first().user.uid)
        assertNull(page.users.first().deleteUrl)
        assertEquals(12, page.pageNav?.totalPages)
    }

    @Test
    fun parseVisitorsAndTrace(): Unit = runBlocking {
        val visitorHtml = loadAsset("user_space/我的好友/我的訪客.html")
        val visitor = assertIs<ParseResult.Success<UserSpaceFriendPage>>(
            UserSpaceFriendPageParser().parse(visitorHtml)
        ).value

        assertEquals(YamiboRoute.UserSpace.FriendPageType.MyVisitor, visitor.type)
        assertEquals(UserId(662515), visitor.users.first().user.uid)

        val traceHtml = loadAsset("user_space/我的好友/我的足跡.html")
        val trace = assertIs<ParseResult.Success<UserSpaceFriendPage>>(
            UserSpaceFriendPageParser().parse(traceHtml)
        ).value

        assertEquals(YamiboRoute.UserSpace.FriendPageType.MyTrace, trace.type)
        assertEquals(UserId(621867), trace.users.first().user.uid)
        assertTrue(trace.users.any { it.description?.contains("个人汉化") == true })
        assertEquals(2, trace.pageNav?.totalPages)
    }

    @Test
    fun parsePrivateMessages(): Unit = runBlocking {
        val html = loadAsset("user_space/我的消息/我的消息.html")
        val result = UserSpacePrivateMessagePageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpacePrivateMessagePage>>(result)
        val page = success.value

        assertEquals(2, page.messages.size)
        assertEquals(1, page.unreadCount)
        val first = page.messages.first()
        assertEquals(UserId(723881), first.user.uid)
        assertEquals("littlesurvival", first.user.name)
        assertEquals("2026-4-29 19:09", first.timeInfo.text)
        assertTrue(first.title.contains("对我"))
        assertEquals("test2", first.message)
        assertEquals(2, first.unreadCount)
        assertNull(page.messages[1].unreadCount)
    }

    @Test
    fun parseNotices(): Unit = runBlocking {
        val html = loadAsset("user_space/我的消息/我的提醒.html")
        val result = UserSpaceNoticePageParser().parse(html)

        val success = assertIs<ParseResult.Success<UserSpaceNoticePage>>(result)
        val page = success.value

        assertTrue(page.notices.isNotEmpty())
        val first = page.notices.first()
        assertEquals(NoticeId(4106095), first.noticeId)
        assertEquals(NoticeType.Post, first.type)
        assertEquals("2026-4-6 18:39", first.timeInfo.text)
        assertTrue(first.contentHtml.contains("黒猫y"))
        assertTrue(first.contentHtml.contains("回复了您的帖子"))
        assertTrue(first.contentHtml.contains("ptid=556584"))
        assertTrue(first.contentHtml.contains("pid=41508969"))

        val rate = page.notices.first { it.type == NoticeType.Rate }
        assertTrue(rate.contentHtml.contains("blockquote"))
        assertTrue(rate.contentHtml.contains("你太可爱"))
        assertEquals("你太可爱", rate.quote)

        val system = page.notices.first { it.type == NoticeType.System }
        assertTrue(system.contentHtml.isNotBlank())

        val friend = page.notices.first { it.type == NoticeType.Friend }
        assertEquals(NoticeId(3979501), friend.noticeId)
        assertTrue(friend.contentHtml.contains("uid=646528"))
    }
}
