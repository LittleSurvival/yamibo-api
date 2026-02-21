import kotlin.test.Test

class NormalKotlinTest {
    internal class Human {
        internal var age: Int = 18
        fun setAge(age: Int) {
            this.age = age
        }
    }

    internal class World(val human: Human) {
        fun printAge() {
            println(human.age)
        }
    }

    @Test
    fun main() {
        val human = Human()
        val world = World(human)

        world.printAge()
        human.setAge(19)
        world.printAge()
    }
}