package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.BlogClassId

/** Selects an existing Discuz blog class or creates one while submitting a blog. */
sealed interface BlogClassSelection {
    data class Existing(val classId: BlogClassId) : BlogClassSelection
    data class Create(val className: String) : BlogClassSelection
}

/** Buffered Discuz response metadata used to verify a blog mutation. */
data class BlogMutationResponse(
    val body: String,
    val statusCode: Int,
    val requestUrl: String,
    val finalUrl: String,
    val location: String?,
)
