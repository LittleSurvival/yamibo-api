package io.github.littlesurvival.dto.page

import kotlinx.serialization.Serializable

/**
 * Rate popout form metadata.
 *
 * @property availableScores Scores selectable in the rate form.
 * @property defaultReasons Preset reason options selectable in the rate form.
 */
@Serializable
data class RatePopoutPage(
    val availableScores: List<Int>,
    val defaultReasons: List<String>,
)
