package io.github.littlesurvival.jvm

import kotlin.test.Test
import kotlin.time.Duration.Companion.hours

class DurationTest {
    @Test
    fun duration() {
        val time = 12.hours
        print(time.inWholeMilliseconds)
    }
}

class ListTest {
    @Test
    fun test() {
        val texts = mutableListOf<String>()
        print(texts[0])
    }
}