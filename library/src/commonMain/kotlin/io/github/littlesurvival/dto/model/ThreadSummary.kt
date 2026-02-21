package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.ThreadId

/** Thread item shown in a forum thread list. */
data class ThreadSummary(
    /** Thread id (tid). */
    val tid: ThreadId,

    /** Thread title. */
    val title: String,

    /** Link to the thread page. */
    val url: String,

    /** Thread Author Info. */
    val author: User? = null,

    /** Thread preview text shown in the list. */
    val description: String? = null,

    /** Number of replies. */
    val replyCount: Int? = null,

    /** Number of views. */
    val viewCount: Int? = null,

    /** Thread tag text (e.g. 公告, 原创). */
    val tag: String? = null,

    /**
     * Last update time text shown in the list.
     *
     * Usually comes from the forum list timestamp (e.g. "2025-12-2 04:41").
     *
     * Kept as raw text to avoid parsing and timezone issues.
     */
    val lastUpdateText: String? = null,
)