package io.github.littlesurvival.fetch

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FetchTest {
    private val client = YamiboClient()

    @Test
    fun test(): Unit = runBlocking {
        val ratePostResult =
            client.fetchRatePost(ThreadId(558130), PostId(41265058), 5, "", FormHash("15f7f9e9"))

        println(ratePostResult.message())
    }
}