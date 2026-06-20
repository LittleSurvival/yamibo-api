package io.github.littlesurvival.parse

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.page.VotersPopoutScreen
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.ThreadId
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class VotersPopoutScreenParserTest {

    private fun loadAsset(name: String): String {
        return this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader(StandardCharsets.UTF_8)
            .readText()
    }

    @Test
    fun parseDefaultSelectedOptionAndVoters(): Unit = runBlocking {
        val result = VotersPopoutScreenParser().parse(loadAsset("vote/voters_popout_screen.html"))

        val screen = assertIs<ParseResult.Success<VotersPopoutScreen>>(result).value
        assertEquals(3, screen.pollOptions.size)
        assertEquals(PollOptionId(34677), screen.selectedPollOptionId)
        assertEquals("架空歷史(非現實古代。)", screen.pollOptions[0].name)
        assertEquals(PollOptionId(34679), screen.pollOptions[2].id)
        assertEquals(3, screen.voters.size)
        assertEquals(173662, screen.voters[0].uid.value)
        assertEquals("ff4686226", screen.voters[0].name)
        assertEquals(145770, screen.voters[1].uid.value)
        assertEquals("面瘫行者", screen.voters[1].name)
    }

    @Test
    fun parseChangedOptionWithNoVoters(): Unit = runBlocking {
        val html = """<?xml version="1.0" encoding="utf-8"?>
            |<root><![CDATA[<div id="floatlayout_viewvote">
            |<select id="polloptionid">
            |<option value="34677">架空歷史</option>
            |<option value="34678" selected="selected">架空正史</option>
            |</select>
            |<ul class="post_box flex-box flex-wrap cl"></ul>
            |</div>]]></root>
        """.trimMargin()

        val result = VotersPopoutScreenParser().parse(html)

        val screen = assertIs<ParseResult.Success<VotersPopoutScreen>>(result).value
        assertEquals(PollOptionId(34678), screen.selectedPollOptionId)
        assertEquals(2, screen.pollOptions.size)
        assertEquals(emptyList(), screen.voters)
    }

    @Test
    fun parsePageNavWhenPopupHasMultiplePages(): Unit = runBlocking {
        val result = VotersPopoutScreenParser().parse(loadAsset("vote/voters_popout_screen_pagenav.html"))

        val screen = assertIs<ParseResult.Success<VotersPopoutScreen>>(result).value
        assertEquals(PageNav(currentPage = 1, nextPageIndex = 2, totalPages = 3, nextUrl = "forum.php?mod=misc&action=viewvote&tid=572624&polloptionid=34713&page=2"), screen.pageNav)
    }

    @Test
    fun rejectResponseWithoutPollOptions(): Unit = runBlocking {
        val html = """<root><![CDATA[<div class="jump_c"><p>投票主題不存在</p></div>]]></root>"""

        val result = VotersPopoutScreenParser().parse(html)

        val failure = assertIs<ParseResult.Failure>(result)
        assertEquals("投票主題不存在", failure.reason)
    }

    @Test
    fun buildViewVotersRoutes() {
        val defaultUrl = YamiboRoute.ViewVoters(ThreadId(572567)).build()
        assertContains(defaultUrl, "action=viewvote")
        assertContains(defaultUrl, "tid=572567")
        assertContains(defaultUrl, "mobile=2")
        assertContains(defaultUrl, "inajax=1")
        assertFalse(defaultUrl.contains("polloptionid="))
        assertFalse(defaultUrl.contains("page="))

        val selectedUrl = YamiboRoute.ViewVoters(ThreadId(572567), PollOptionId(34678)).build()
        assertContains(selectedUrl, "polloptionid=34678")

        val pagedUrl = YamiboRoute.ViewVoters(ThreadId(572567), PollOptionId(34678), 2).build()
        assertContains(pagedUrl, "polloptionid=34678")
        assertContains(pagedUrl, "page=2")
    }
}
