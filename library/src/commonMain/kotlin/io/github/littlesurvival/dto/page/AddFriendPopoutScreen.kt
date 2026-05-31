package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.User
import kotlinx.serialization.Serializable

/**
 * Add-friend popout form data.
 *
 * @property user Target user shown in the popout.
 * @property availableOption Friend group options selectable in the form.
 */
@Serializable
data class AddFriendPopoutScreen(
    val user: User,
    val availableOption: List<AddFriendOption>,
)

/**
 * Friend group option in the add-friend form.
 *
 * @property id Group id submitted as `gid`.
 * @property reason Display name of the friend group.
 */
@Serializable
data class AddFriendOption(
    val id: Int,
    val reason: String,
)

