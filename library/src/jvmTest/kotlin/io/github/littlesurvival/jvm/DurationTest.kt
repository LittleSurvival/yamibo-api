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
        val list = arrayListOf("a", "b", "c", "d", "e", "f")
        var retryCount = 3
        while (retryCount-- > 0) {
            val random = list.random()
            println("current random alphabet $random")
            println("current list : $list")
            if (random == "d") {
                return
            } else {
                list.remove(random)
            }
        }
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