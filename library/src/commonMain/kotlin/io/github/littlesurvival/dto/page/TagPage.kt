package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary
import kotlinx.serialization.Serializable

@Serializable
data class TagPage(
    val threadSummaries : List<ThreadSummary>,
    val pageNav: PageNav? = null
)