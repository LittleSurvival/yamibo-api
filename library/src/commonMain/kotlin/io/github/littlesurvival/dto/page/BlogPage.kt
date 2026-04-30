package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.value.BlogCommentId
import io.github.littlesurvival.dto.value.BlogId
import kotlinx.serialization.Serializable

/**
 * Blog detail page.
 *
 * @property blogInfo Blog metadata shown by Yamibo.
 * @property rootBlog Root blog post content.
 * @property blogComments Comments under the blog.
 * @property pageNav Pagination info for comments, if present.
 */
@Serializable
data class BlogPage(
    val blogInfo: BlogInfo,
    val rootBlog: BlogComment,
    val blogComments: List<BlogComment>,
    val pageNav: PageNav? = null
)

/**
 * Blog metadata.
 *
 * @property blogId Blog ID.
 * @property title Blog title.
 * @property totalReplies Total comment count, if present.
 * @property totalViews Total view count, if present.
 * @property collectUrl URL for adding the blog to favorites.
 * @property shareUrl URL for sharing the blog.
 * @property inviteUrl URL for inviting others to view the blog.
 */
@Serializable
data class BlogInfo(
    val blogId: BlogId,
    val title: String,
    val totalReplies: Int? = null,
    val totalViews: Int? = null,

    /**
     * Get from Root Blog.
     */
    val collectUrl: String? = null,
    val shareUrl: String? = null,
    val inviteUrl: String? = null,
)

/**
 * Blog root post or comment item.
 *
 * @property bcId Blog comment ID. Null when this item is the root blog post.
 * @property author Author info.
 * @property contentHtml Raw HTML content.
 * @property replyUrl URL used to reply to this comment, if present.
 * @property timeInfo Create time.
 */
@Serializable
data class BlogComment(
    val bcId: BlogCommentId? = null,
    val author: User,
    val contentHtml: String,
    val replyUrl: String?,
    val timeInfo: TimeInfo,
)
