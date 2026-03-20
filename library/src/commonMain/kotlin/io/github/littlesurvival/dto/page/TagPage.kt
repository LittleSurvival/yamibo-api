package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary

/**
 * Support :
 * - Tid
 * - Thread Title
 * - Url
 * - Fid
 * - Author
 * - AttachmentType
 * - ReplyCount
 * - ViewCount
 * - LastUpdateText
 */
data class TagPage(
    val threadSummaries : List<ThreadSummary>,
    val pageNav: PageNav? = null
)