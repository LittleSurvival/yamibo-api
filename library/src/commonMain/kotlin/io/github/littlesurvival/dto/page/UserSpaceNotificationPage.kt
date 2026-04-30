package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.value.NoticeId
import kotlinx.serialization.Serializable

/**
 * User-space private-message list page.
 *
 * @property messages Private-message conversation previews.
 * @property unreadCount Total unread private-message count shown on the tab.
 * @property pageNav Pagination info, if present.
 */
@Serializable
data class UserSpacePrivateMessagePage(
    val messages: List<PrivateMessageItem>,
    val unreadCount: Int? = null,
    val pageNav: PageNav? = null
)

/**
 * Private-message conversation preview.
 *
 * @property user Conversation target user.
 * @property conversationUrl URL to open the conversation.
 * @property timeInfo Last message time.
 * @property title Raw title text shown by Yamibo, e.g. "name 对我 说:".
 * @property message Last message preview.
 * @property unreadCount Unread count for this conversation, if present.
 */
@Serializable
data class PrivateMessageItem(
    val user: User,
    val conversationUrl: String,
    val timeInfo: TimeInfo,
    val title: String,
    val message: String,
    val unreadCount: Int? = null
)

/**
 * User-space notice list page.
 *
 * @property notices Notice items shown on the page.
 * @property pageNav Pagination info, if present.
 */
@Serializable
data class UserSpaceNoticePage(
    val notices: List<NoticeItem>,
    val pageNav: PageNav? = null
)

/**
 * Notice item shown in "My notice".
 *
 * @property noticeId Notice ID.
 * @property type Notice type parsed from Yamibo's ignore URL.
 * @property avatarUrl Notice avatar URL, including system icon URL.
 * @property timeInfo Notice time.
 * @property contentHtml Raw notice body HTML.
 * @property ignoreUrl URL used to ignore/block this notice type.
 * @property quote Quote/reason text, usually present in rate notices.
 */
@Serializable
data class NoticeItem(
    val noticeId: NoticeId,
    val type: NoticeType,
    val avatarUrl: String?,
    val timeInfo: TimeInfo,
    val contentHtml: String,
    val ignoreUrl: String? = null,
    val quote: String? = null
)

/**
 * Notice type.
 */
@Serializable
enum class NoticeType {
    Post,
    PostComment,
    Rate,
    Friend,
    System,
    Unknown
}
