package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.TimeInfo
import kotlinx.serialization.Serializable

/**
 * All-ratings popout page.
 *
 * @property totalScore Total score shown in the footer, when available.
 * @property rates Parsed rating entries shown in the popup.
 * @property pageNav Page navigation, if the popup is paginated.
 */
@Serializable
data class RateResultPopoutPage(
    val totalScore: Int? = null,
    val rates: List<RateResultItem>,
    val pageNav: PageNav? = null,
)

/**
 * Single rating entry in the all-ratings popup.
 *
 * @property score Score given by the user.
 * @property userName Rater username.
 * @property timeInfo Time shown on the row.
 * @property reason Optional reason row attached to the score entry.
 */
@Serializable
data class RateResultItem(
    val score: Int,
    val userName: String,
    val timeInfo: TimeInfo,
    val reason: String? = null,
)
