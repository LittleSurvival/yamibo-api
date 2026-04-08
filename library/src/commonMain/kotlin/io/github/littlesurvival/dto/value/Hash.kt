package io.github.littlesurvival.dto.value

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** Type-safe form hash token. */
@Serializable
@JvmInline
value class FormHash(val value: String)