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

class Division {
    @Test
    fun test() {
        val x = 24L
        val y = 5

        val z = 60L


        println((x / y.toFloat() * z).toLong())
    }
}