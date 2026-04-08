package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.TagId
import kotlinx.serialization.Serializable

@Serializable
data class Tags(
    val value: List<TagValue> = emptyList()
)

@Serializable
data class TagValue(
    val id: TagId,
    val name: String,
)