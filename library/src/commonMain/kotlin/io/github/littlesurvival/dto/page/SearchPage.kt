package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary

data class SearchPage(
    val threads: List<ThreadSummary>,
    val totalCount: Int,
    val pageNav: PageNav? = null
)