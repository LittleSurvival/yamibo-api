package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.BlogId
import kotlinx.serialization.Serializable

/**
 * Blog summary item shown in user-space blog lists.
 *
 * @property title Blog title.
 * @property bId Blog ID.
 * @property url Blog detail URL.
 * @property description Blog preview text.
 * @property author Blog author info.
 * @property timeInfo Blog publish/update time.
 */
@Serializable
data class BlogSummary(
    val title: String,
    val bId: BlogId,
    val url: String,
    val description: String,
    val author: User,
    val timeInfo: TimeInfo,
)
