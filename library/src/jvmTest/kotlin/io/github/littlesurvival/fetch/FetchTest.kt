package io.github.littlesurvival.fetch

import debugLog
import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.dto.value.ThreadId
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
        val forumResult = client.fetchForumById(YamiboForum.TRANSLATED_LIGHT_NOVEL.id)
        debugLog("fetchForumById", forumResult)
    }

    @Test
    fun testAddForum() : Unit = runBlocking {
        val favoriteResult = client.fetchAddFavorite(ForumId(55), formHash)
        debugLog("fetchAddFavorite", favoriteResult)
    }

    @Test
    fun testFavorite(): Unit = runBlocking {
        val favoriteResult = client.fetchFavorite(type = FavoriteType.Thread, page = 1)
        debugLog("fetchFavorite", favoriteResult)
    }

    @Test
    fun testThread(): Unit = runBlocking {
        // 568055, 535612, 564532, 565033, 557223, 535057
        val threadResult = client.fetchThreadById(ThreadId(535057), page = 1)
        if (threadResult is YamiboResult.Success) {
            println("post size : ${threadResult.value.posts.size}")
        }
        debugLog("fetchThreadById", threadResult)
    }

    @Test
    fun testFindPost(): Unit = runBlocking {
        val findPostResult = client.fetchFindPost(postId = PostId(41251744))
        debugLog("fetchFindPost", findPostResult)
    }

    @Test
    fun testRatePost(): Unit = runBlocking {
        val ratePostResult = client.fetchRatePost(ThreadId(565238), PostId(41426069), 5, "", formHash)
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
}
