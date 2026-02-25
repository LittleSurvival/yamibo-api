package io.github.littlesurvival.render

import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.Attachment
import io.github.littlesurvival.dto.value.PostId

/**
 * Callbacks exposed by [PostRenderer] for the host App to handle user interactions.
 *
 * The renderer itself performs **no** navigation, database access, or state mutation — every
 * interactive event is forwarded through this interface.
 *
 * All callbacks have default no-op implementations so callers only need to override the
 * interactions they care about.
 */
interface PostRendererCallbacks {

    /** Called when the user taps the author avatar or name. */
    fun onUserClick(user: User) {}

    /** Called when the user taps a hyperlink (site-internal or external). */
    fun onLinkClick(href: String) {}

    /** Called when the user taps an image. */
    fun onImageClick(url: String) {}

    /** Called when the user taps an attachment download link. */
    fun onAttachmentClick(attachment: Attachment) {}

    /** Called when the user taps the "评分" (Rate) button. */
    fun onRateButtonClick(pid: PostId) {}

    /** Called when the user taps the "点评" (Comment) button. */
    fun onCommentButtonClick(pid: PostId) {}

    /** Called when the calculated reading anchor changes. */
    fun onAnchorChanged(anchor: PostReadingAnchor) {}
}

/**
 * Empty / no-op default implementation of [PostRendererCallbacks].
 *
 * Useful as a base when only a subset of callbacks is needed:
 * ```kotlin
 * val cb = object : EmptyPostRendererCallbacks() {
 *     override fun onLinkClick(href: String) { /* handle */ }
 * }
 * ```
 */
open class EmptyPostRendererCallbacks : PostRendererCallbacks
