package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable

/** Forum user information. */
@Serializable
data class User(
    /** User id (uid). */
    val uid: UserId,

    /** User display name. */
    val name: String,

    /** URL to the user's avatar image. */
    val avatarUrl: String? = null
)
