package io.github.littlesurvival.render

import io.github.littlesurvival.dto.value.PostId

/**
 * Reading-position anchor within a single [PostRenderer].
 *
 * Used to persist and restore the user's scroll position at the thread level. The outer
 * [LazyColumn] stores `(pid, anchor)` pairs; when coming back to the thread the app scrolls to the
 * correct post and then asks [PostRenderer] to bring the matching block into view.
 *
 * @property pid The post this anchor belongs to.
 * @property blockIndex Zero-based index of the content block that overlaps
 * ```
 *                       the anchor line.
 * @property intraOffset
 * ```
 * Reserved for sub-block offset (e.g. paragraph line
 * ```
 *                       within a long text block). Defaults to 0.
 * ```
 */
data class PostReadingAnchor(val pid: PostId, val blockIndex: Int, val intraOffset: Int = 0)
