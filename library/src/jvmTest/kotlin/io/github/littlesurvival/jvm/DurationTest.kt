package io.github.littlesurvival.jvm

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours

class DurationTest {
    @Test
    fun duration() {
        val time = 12.hours
        print(time.inWholeMilliseconds)
    }
}