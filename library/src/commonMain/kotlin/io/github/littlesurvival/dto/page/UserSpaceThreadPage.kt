package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.serialization.Serializable

/**
 * User-space thread list page for "Ta's threads".
 *
 * @property threads Thread summaries shown on the page.
 * @property pageNav Pagination info, if present.
 */
@Serializable
data class UserSpaceThreadPage(
    val threads: List<ThreadSummary>,
    val pageNav: PageNav? = null
)

/**
 * User-space reply list page for "Ta's replies".
 *
 * @property replies Reply groups by source thread.
 * @property pageNav Pagination info, if present.
 */
@Serializable
data class UserSpaceThreadReplyPage(
    val replies: List<ReplyItem>,
    val pageNav: PageNav? = null
)

/**
 * Reply group under one source thread.
 *
 * @property title Source thread title.
 * @property tId Source thread ID.
 * @property url Source thread find-post URL.
 * @property posts Reply post previews under this thread.
 */
@Serializable
data class ReplyItem(
    val title: String,
    val tId: ThreadId,
    val url: String,
    val posts: List<ReplyPostItems>
)

/**
 * Reply post preview item.
 *
 * @property tId Source thread ID.
 * @property pId Reply post ID.
 * @property url URL to locate this reply post.
 * @property quote Reply preview text.
 */
@Serializable
data class ReplyPostItems(
    val tId: ThreadId,
    val pId: PostId,
    val url: String,
    val quote: String,
)
