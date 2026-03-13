import io.github.littlesurvival.core.YamiboResult
import java.lang.reflect.Modifier
import kotlin.jvm.java

/**
 * Pretty-print a YamiboResult for debugging.
 *
 * Maps each result type and prints a clear, labeled output.
 */
internal fun <T> debugLog(label: String, result: YamiboResult<T>) {
    val divider = "═".repeat(60)
    println("\n$divider")
    println("📋 $label")
    println(divider)
    when (result) {
        is YamiboResult.Success -> {
            println("✅ [Success]")
            println("   Type : ${result.value!!::class.simpleName}")
            println()
            println(toKotlinCode(result.value))
        }
        is YamiboResult.Failure -> {
            println("❌ [Failure]")
            result.reason.lines().forEach { println("   $it") }
            result.exception?.let { ex ->
                println("   Exception: ${ex::class.simpleName} - ${ex.message}")
            }
        }
        is YamiboResult.Maintenance -> {
            println("🔧 [Maintenance]")
            println("   ${result.message()}")
        }
        is YamiboResult.NotLoggedIn -> {
            println("🔒 [NotLoggedIn]")
            println("   ${result.message()}")
        }
        is YamiboResult.NoPermission -> {
            println("🔒 [NoPermission]")
            println("   ${result.message()}")
        }
    }
    println("$divider\n")
}

/** Converts any object (DTOs, Lists, basic types) into copyable Kotlin code. */
internal fun toKotlinCode(obj: Any?, indentSize: Int = 4, depth: Int = 0): String {
    val indent = " ".repeat(depth * indentSize)
    if (obj == null) return "null"
    if (obj is String) {
        return if ("\n" in obj) {
            val lines = obj.split("\n")
            val nextIndent = " ".repeat((depth + 1) * indentSize)
            val joined = lines.joinToString("\n$nextIndent") { it.replace("$", "\${'$'}") }
            "\"\"\"\n$nextIndent$joined\n$indent\"\"\".trimIndent()"
        } else {
            "\"${obj.replace("\"", "\\\"").replace("$", "\${'$'}")}\""
        }
    }
    if (obj is Number || obj is Boolean) {
        return obj.toString()
    }
    if (obj is Char) {
        val escaped =
                when (obj) {
                    '\'' -> "\\'"
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    '\\' -> "\\\\"
                    else -> obj.toString()
                }
        return "'$escaped'"
    }
    if (obj is Enum<*>) {
        return "${obj::class.simpleName}.${obj.name}"
    }
    if (obj is List<*>) {
        if (obj.isEmpty()) return "emptyList()"
        val nextIndentStr = " ".repeat((depth + 1) * indentSize)
        return obj.joinToString(prefix = "listOf(\n", postfix = "\n$indent)", separator = ",\n") {
                item ->
            nextIndentStr + toKotlinCode(item, indentSize, depth + 1)
        }
    }
    if (obj is Set<*>) {
        if (obj.isEmpty()) return "emptySet()"
        val nextIndentStr = " ".repeat((depth + 1) * indentSize)
        return obj.joinToString(prefix = "setOf(\n", postfix = "\n$indent)", separator = ",\n") {
                item ->
            nextIndentStr + toKotlinCode(item, indentSize, depth + 1)
        }
    }
    if (obj is Map<*, *>) {
        if (obj.isEmpty()) return "emptyMap()"
        val nextIndentStr = " ".repeat((depth + 1) * indentSize)
        return obj.entries.joinToString(
                prefix = "mapOf(\n",
                postfix = "\n$indent)",
                separator = ",\n"
        ) { (k, v) ->
            nextIndentStr +
                    "${toKotlinCode(k, indentSize, depth + 1)} to ${toKotlinCode(v, indentSize, depth + 1)}"
        }
    }

    val clazz = obj::class.java
    val simpleName = clazz.simpleName
    val fields =
            clazz.declaredFields.filter {
                !Modifier.isStatic(it.modifiers) &&
                        !it.name.startsWith("$") &&
                        !it.name.startsWith("this$")
            }
    if (fields.isEmpty()) {
        val str = obj.toString()
        // If it's just some java.lang.Object or standard class with a string representation, return
        // it as string
        return if (str.startsWith(clazz.name ?: "") || simpleName.isNullOrBlank()) {
            "\"${str.replace("\"", "\\\"")}\""
        } else {
            "$simpleName()"
        }
    }

    val nextIndentStr = " ".repeat((depth + 1) * indentSize)
    return fields.joinToString(
            prefix = "$simpleName(\n",
            postfix = "\n$indent)",
            separator = ",\n"
    ) { field ->
        field.isAccessible = true
        val value =
                try {
                    field.get(obj)
                } catch (e: Exception) {
                    "\"<error>\""
                }
        "$nextIndentStr${field.name} = ${toKotlinCode(value, indentSize, depth + 1)}"
    }
}
