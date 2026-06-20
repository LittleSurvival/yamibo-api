package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.value.PollOptionId
import kotlinx.serialization.Serializable

/**
 * Popout screen listing the users who voted for one poll option.
 *
 * @property pollOptions All poll options available in the selector.
 * @property selectedPollOptionId Option whose voters are currently displayed.
 * @property voters Users who voted for the selected option.
 */
@Serializable
data class VotersPopoutScreen(
    val pollOptions: List<VotersPollOption>,
    val selectedPollOptionId: PollOptionId,
    val voters: List<User>,
)

/**
 * Poll option shown in the voters popout selector.
 *
 * @property id Poll option id submitted as `polloptionid`.
 * @property name Poll option display text.
 */
@Serializable
data class VotersPollOption(
    val id: PollOptionId,
    val name: String,
)
