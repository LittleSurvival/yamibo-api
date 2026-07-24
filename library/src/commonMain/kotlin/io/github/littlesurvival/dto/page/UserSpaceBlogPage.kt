package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.BlogSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.value.BlogClassId
import kotlinx.serialization.Serializable

/**
 * User-space blog list page.
 *
 * @property blogs Blog summaries shown on the page.
 * @property pageNav Pagination info, if present.
 * @property blogClasses User blog classes shown in the horizontal class list.
 */
@Serializable
data class UserSpaceBlogPage(
    val blogs: List<BlogSummary>,
    val pageNav: PageNav? = null,
    val blogClasses: List<BlogPageClassInfo> = emptyList(),
)

/**
 * Blog class shown on user-space blog list pages.
 *
 * @property name Blog class display name.
 * @property id Blog class ID.
 */
@Serializable
data class BlogPageClassInfo(
    val name: String,
    val id: BlogClassId,
)
