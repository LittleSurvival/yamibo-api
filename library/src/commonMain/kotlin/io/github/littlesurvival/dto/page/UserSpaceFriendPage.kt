package io.github.littlesurvival.dto.page

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.User
import kotlinx.serialization.Serializable

/**
 * User-space friend-related list page.
 *
 * @property type Current friend page type.
 * @property users User items shown on the page.
 * @property pageNav Pagination info, if present.
 */
@Serializable
data class UserSpaceFriendPage(
    val type: YamiboRoute.UserSpace.FriendPageType,
    val users: List<UserSpaceFriendItem>,
    val pageNav: PageNav? = null
)

/**
 * User item shown in friend, online member, visitor, or trace lists.
 *
 * @property user User info.
 * @property profileUrl User profile URL.
 * @property pmUrl Private-message URL, if present.
 * @property deleteUrl Delete-friend URL, only present on "My friend" page.
 * @property description Extra trace/visitor text, if present.
 */
@Serializable
data class UserSpaceFriendItem(
    val user: User,
    val profileUrl: String,
    val pmUrl: String? = null,
    val deleteUrl: String? = null,
    val description: String? = null
)
