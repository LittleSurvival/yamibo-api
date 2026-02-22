package io.github.littlesurvival.fetch

import debugLog
import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import kotlin.test.Test
import kotlinx.coroutines.runBlocking

class FetchTest {

    private fun loadAsset(name: String): String =
        this::class.java.classLoader!!.getResourceAsStream("assets/$name")!!
            .bufferedReader()
            .readText()
            .replace("\n", "")
            .trim()
    private val client by lazy { YamiboClient().also {
        it.setCookie(loadAsset("secret/cookie"))
    }}
    private val formHash by lazy { FormHash(loadAsset("secret/formhash")) }

    @Test
    fun test(): Unit = runBlocking {
        val ratePostResult = client.fetchRatePost(ThreadId(565238), PostId(41426069), 5, "", formHash)
        debugLog("fetchRatePost", ratePostResult)
    }

    @Test
    fun testSearch() = runBlocking {
        val query = "姊妹"
        val result = client.fetchSearch(query, null,formHash)
        debugLog("fetchSearch(\"$query\")", result)
    }

    @Test
    fun testSetHomePage() = runBlocking {
        val homePage = client.fetchHomePage()
        debugLog("fetchHomePage", homePage)
    }
}
