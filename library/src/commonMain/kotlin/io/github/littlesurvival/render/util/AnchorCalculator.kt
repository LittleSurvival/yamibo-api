package io.github.littlesurvival.render.util

import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.render.AnchorLineSpec
import io.github.littlesurvival.render.PostReadingAnchor

/**
 * Pure-logic helper that determines which content-block index currently overlaps the anchor line.
 *
 * **Algorithm — "anchor-line overlay":**
 * 1. A virtual horizontal anchor line is placed at a fixed offset from the
 * ```
 *    top of the viewport (or a percentage of the viewport height).
 * ```
 * 2. For every measured block we know its `topY` and `bottomY` in window
 * ```
 *    coordinates.
 * ```
 * 3. If a block straddles the anchor line (`topY <= anchorLine < bottomY`),
 * ```
 *    that block is the anchor.
 * ```
 * 4. If no block straddles it (e.g. between two blocks), pick the one whose
 * ```
 *    edge is closest to the anchor line.
 * ```
 * The caller is responsible for collecting measurements from Compose layout and forwarding them
 * here.
 */
object AnchorCalculator {

    /**
     * Measured position of a single content block in window coordinates.
     *
     * @property index Zero-based block index.
     * @property topY Top edge Y in pixels (window coordinates).
     * @property bottomY Bottom edge Y in pixels (window coordinates).
     */
    data class BlockMeasurement(val index: Int, val topY: Float, val bottomY: Float)

    /**
     * Resolve the anchor-line Y position in pixels.
     *
     * @param spec The [AnchorLineSpec] from [RenderConfig].
     * @param viewportHeight Total viewport height in pixels.
     * @param density Current screen density (px per dp).
     * @return Anchor line Y in pixels (window coordinates).
     */
    fun resolveAnchorLineY(spec: AnchorLineSpec, viewportHeight: Float, density: Float): Float {
        return if (spec.useRatio) {
            viewportHeight * spec.ratio
        } else {
            spec.offsetDp * density
        }
    }

    /**
     * Calculate which block is at the anchor line.
     *
     * @param pid Post id this card belongs to.
     * @param measurements Ordered list of block measurements (must be
     * ```
     *                     non-empty).
     * @param anchorLineY
     * ```
     * Anchor line position in pixels (window coords).
     * @return The computed [PostReadingAnchor], or `null` if [measurements]
     * ```
     *         is empty.
     * ```
     */
    fun calculate(
            pid: PostId,
            measurements: List<BlockMeasurement>,
            anchorLineY: Float
    ): PostReadingAnchor? {
        if (measurements.isEmpty()) return null

        // Case 1: find a block that straddles the anchor line
        for (m in measurements) {
            if (m.topY <= anchorLineY && anchorLineY < m.bottomY) {
                // Calculate intra-block offset as a proportion (0..1000)
                val blockHeight = m.bottomY - m.topY
                val intra =
                        if (blockHeight > 0f) {
                            ((anchorLineY - m.topY) / blockHeight * 1000).toInt().coerceIn(0, 1000)
                        } else {
                            0
                        }
                return PostReadingAnchor(pid, m.index, intra)
            }
        }

        // Case 2: no block straddles — pick the closest edge
        var bestIndex = 0
        var bestDist = Float.MAX_VALUE
        for (m in measurements) {
            val distTop = kotlin.math.abs(m.topY - anchorLineY)
            val distBot = kotlin.math.abs(m.bottomY - anchorLineY)
            val dist = minOf(distTop, distBot)
            if (dist < bestDist) {
                bestDist = dist
                bestIndex = m.index
            }
        }
        return PostReadingAnchor(pid, bestIndex, 0)
    }
}
