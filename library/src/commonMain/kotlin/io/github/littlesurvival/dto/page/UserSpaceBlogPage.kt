package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.BlogSummary
import io.github.littlesurvival.dto.model.PageNav
import kotlinx.serialization.Serializable

/**
 * User-space blog list page.
 *
 * @property blogs Blog summaries shown on the page.
 * @property pageNav Pagination info, if present.
 */
@Serializable
data class UserSpaceBlogPage(
    val blogs: List<BlogSummary>,
    val pageNav: PageNav? = null
)
