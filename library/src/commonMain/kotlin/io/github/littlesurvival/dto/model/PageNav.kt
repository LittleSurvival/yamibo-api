package io.github.littlesurvival.dto.model

import kotlinx.serialization.Serializable

/**
 * Pagination navigation URLs for multipage listings.
 *
 * Used by both forum pages and thread pages.
 */
@Serializable
data class PageNav(
    /** URL to the next page, if available. */
    val nextUrl: String? = null,

    /** Next page index parsed from [nextUrl], if available. */
    val nextPageIndex: Int? = null,

    /** URL to the previous page, if available. */
    val prevUrl: String? = null,

    /** Previous page index parsed from [prevUrl], if available. */
    val prevPageIndex: Int? = null,

    /**
     * Current page number (1-based).
     *
     * Parsed from the highlighted `<strong>` element or the input value in the pagination
     * widget.
     */
    val currentPage: Int? = null,

    /**
     * Total number of pages.
     *
     * Parsed from the `<span title="共 N 页">` element in the pagination widget.
     */
    val totalPages: Int? = null
)
