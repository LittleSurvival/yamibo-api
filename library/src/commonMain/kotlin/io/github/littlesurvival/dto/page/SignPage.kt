package io.github.littlesurvival.dto.page

import kotlinx.serialization.Serializable

/**
 * Daily sign-in page.
 *
 * @property currentDateText Current date text displayed by Yamibo.
 * @property monthLabel Calendar month label.
 * @property notice Sign-in notice or rule text, if present.
 * @property calendarDays Calendar cells parsed from the sign-in page.
 * @property repairOptions Available repair-sign options.
 * @property myActivity Recent personal sign-in activity.
 * @property statistics Sign-in statistics shown by Yamibo.
 * @property extraSections Other info sections on the sign-in page.
 * @property signActionUrl Absolute URL for the primary sign-in action.
 * @property repairActionPrefix Absolute URL prefix for repair sign actions. Append a repair option value.
 * @property hasSignedToday Whether the current user appears to have signed today.
 * @property lastSignDateKey Last parsed sign date in `yyyy-MM-dd` format, if available.
 */
@Serializable
data class SignPage(
    val currentDateText: String? = null,
    val monthLabel: String? = null,
    val notice: String? = null,
    val calendarDays: List<SignCalendarDay> = emptyList(),
    val repairOptions: List<SignRepairOption> = emptyList(),
    val myActivity: List<String> = emptyList(),
    val statistics: List<String> = emptyList(),
    val extraSections: List<SignInfoSection> = emptyList(),
    val signActionUrl: String? = null,
    val repairActionPrefix: String? = null,
    val hasSignedToday: Boolean = false,
    val lastSignDateKey: String? = null,
)

/**
 * A day cell in the sign-in calendar.
 */
@Serializable
data class SignCalendarDay(
    val day: Int,
    val isSigned: Boolean,
    val isToday: Boolean,
)

/**
 * Repair-sign dropdown option.
 */
@Serializable
data class SignRepairOption(
    val value: String,
    val label: String,
)

/**
 * Generic sign-page info section.
 */
@Serializable
data class SignInfoSection(
    val title: String,
    val items: List<String>,
)

/**
 * Result of a sign or repair-sign action.
 */
@Serializable
data class SignActionResult(
    val status: SignActionStatus,
    val message: String,
)

/**
 * Sign action status inferred from Yamibo's result message.
 */
@Serializable
enum class SignActionStatus {
    Success,
    AlreadySigned,
    RepairSuccess,
}
