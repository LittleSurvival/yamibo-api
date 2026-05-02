package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.value.PrivateMessageId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable

/**
 * Private-message conversation page.
 *
 * @property toUser Target user ID in this conversation.
 * @property title Page title shown by Yamibo.
 * @property pmId Private-message thread ID from `#pmform` action.
 * @property messages Messages shown on the current page.
 * @property pageNav Pagination info, if present.
 */
@Serializable
data class PrivateMessagePage(
    val toUser: UserId,
    val title: String,
    val pmId: PrivateMessageId,
    val messages: List<PrivateMessage>,
    val pageNav: PageNav? = null,
)

/**
 * Private-message item.
 *
 * @property user Sender info.
 * @property messageType Whether the message is sent by self or the friend.
 * @property contentHtml Raw message body HTML.
 * @property timeInfo Message time.
 */
@Serializable
data class PrivateMessage(
    val user: User,
    val messageType: PrivateMessageType,
    val contentHtml: String,
    val timeInfo: TimeInfo
)

/**
 * Private-message sender type.
 */
@Serializable
enum class PrivateMessageType {
    Self,
    Friend
}
