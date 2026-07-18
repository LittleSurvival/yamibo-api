package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.page.ManageButton
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
 * @property manageButtons Management/action buttons shown for this blog.
 */
@Serializable
data class BlogSummary(
    val title: String,
    val bId: BlogId,
    val url: String,
    val description: String,
    val author: User,
    val timeInfo: TimeInfo,
    val manageButtons: List<ManageButton> = emptyList(),
)
