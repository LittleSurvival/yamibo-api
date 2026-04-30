package io.github.littlesurvival.fetch

import debugLog
import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FetchTest {

    private fun loadAsset(name: String): String =
        this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader()
            .readText()
            .replace("\n", "")
            .trim()
    private val client = YamiboClient(timeoutMillis = 60_000L).also {
        it.setCookie(loadAsset("secret/cookie"))
    }
    private val formHash = FormHash(loadAsset("secret/formhash"))

    @Test
    fun testProfile(): Unit = runBlocking {
        val profileResult = client.fetchProfileInfo()
        debugLog("fetchProfileInfo", profileResult)
    }

    @Test
    fun testForum(): Unit = runBlocking {
        val forumResult = client.fetchForumById(YamiboForum.SEA.forumId)
        debugLog("fetchForumById", forumResult)
    }

    @Test
    fun testAddForum() : Unit = runBlocking {
        val favoriteResult = client.fetchAddFavorite(ForumId(55), formHash)
        debugLog("fetchAddFavorite", favoriteResult)
    }

    @Test
    fun testFavorite(): Unit = runBlocking {
        val favoriteResult = client.fetchFavoritePage(type = FavoriteType.Thread, page = 1)
        debugLog("fetchFavorite", favoriteResult)

        if (favoriteResult is YamiboResult.Success) {
            val ids = favoriteResult.value.items.map { it.favId.also { id -> println(id) } }
            var isSort = true
            for (i in ids.indices) {
                if (i == ids.size - 1) break
                if (ids[i].value < ids[i + 1].value) isSort = false
            }
            println("isSort : $isSort")
        }
    }

    @Test
    fun testRemoveFavorite(): Unit = runBlocking {
        val removeFavorite = client.fetchRemoveFavorite(FavoriteId(2549313), formHash)
        debugLog("fetchRemoveFavorite", removeFavorite)
    }

    @Test
    fun testThread(): Unit = runBlocking {
        // 568055, 535612, 564532, 565033, 557223, 535057, 568285, 567394, 568356, 568493, 566241, 551879, 568644, 568760, 547803, 555740, 535802
        val threadResult = client.fetchThreadById(ThreadId(535802), page = 1)
        if (threadResult is YamiboResult.Success) {
            println("post size : ${threadResult.value.posts.size}")
        }
        debugLog("fetchThreadById", threadResult)
    }

    @Test
    fun testFindPost(): Unit = runBlocking {
        val findPostResult = client.fetchFindPost(postId = PostId(41206202))
        debugLog("fetchFindPost", findPostResult)
    }

    @Test
    fun testRatePost(): Unit = runBlocking {
        val ratePostResult = client.fetchRatePost(ThreadId(564532), PostId(41404794), 5, "", formHash)
        debugLog("fetchRatePost", ratePostResult)
    }

    @Test
    fun testSearch() = runBlocking {
        val query = "百合"
        val result = client.fetchSearch(query, null, formHash)
        debugLog("fetchSearch(\"$query\")", result)
    }

    @Test
    fun testSearchById() = runBlocking {
        val id = SearchId(44662)
        val result = client.fetchSearchById("", id, 1)
        debugLog("fetchSearchById(\"\", $id, 1)", result)
    }

    @Test
    fun testSetHomePage() = runBlocking {
        val homePage = client.fetchHomePage()
        debugLog("fetchHomePage", homePage)
    }

    @Test
    fun testVotePoll() = runBlocking {
        val pollResult = client.votePoll(
            YamiboForum.SEA.forumId,
            ThreadId(559877),
            listOf(PollOptionId(32952), PollOptionId(32954)),
            formHash
        )
        debugLog("votePoll", pollResult)
    }

    @Test
    fun testExtractTag() = runBlocking {
        //568644, 568611
        val tagResult = client.fetchExtractTagsInThreadById(ThreadId(568611))

        debugLog("Tags", tagResult)
    }

    @Test
    fun testTagPage() = runBlocking {
        val result = client.fetchTagPageById(TagId(20666))

        debugLog("TagPage", result)
    }

    @Test
    fun testRatePopoutPage() = runBlocking {
        val result = client.fetchRatePopoutPage(ThreadId(564532), PostId(41404794))

        debugLog("RatePopoutPage", result)
    }

    @Test
    fun testUserSpaceThreadPage() = runBlocking {
        val result = client.fetchUserSpaceThreads(userId = UserId(121222))

        debugLog("UserSpaceThreadPage", result)
    }
}