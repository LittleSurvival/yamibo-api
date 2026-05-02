package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.dto.model.TimeInfo
import kotlinx.serialization.Serializable

/**
 * Profile page data model.
 *
 * @property uid User ID.
 * @property username Username(e.g. "thenano").
 * @property userGroup User group(e.g. "百合花蕾").
 * @property adminGroup Admin group(管理組, e.g. "管理員"), null if not an admin.
 * @property points Points(積分).
 * @property partner Objects(對象).
 * @property totalPoints Total points(總積分).
 * @property avatarUrl Avatar URL(e.g. "uc_server/data/avatar/000/65/66/26_avatar_big.jpg").
 * @property avatarBackgroundUrl Avatar background URL from profile header style.
 * @property signatureHtml Personal signature raw HTML(個人簽名), null if not set.
 * @property gender Gender(性別, e.g. "保密").
 * @property birthday Birthday(生日, e.g. "2000-1-1"), null if not set.
 * @property birthplace Birthplace(出生地, e.g. "中国 广东省"), null if not set.
 * @property education Education(學歷, e.g. "本科"), null if not set.
 * @property customTitle Custom title(自定義頭銜), null if not set.
 * @property homepage Personal homepage URL(個人主頁), null if not set.
 * @property onlineHours Online hours(在線時間, e.g. 172).
 * @property registerTime Registration time(注冊時間, e.g. "2024-8-14 20:23").
 * @property lastVisit Last visit time(最後訪問, e.g. "2026-2-24 00:49").
 * @property formHash Form hash(Important Info, use for all post request.).
 */
@Serializable
data class ProfilePage(
    val uid: UserId,
    val username: String,
    val userGroup: String,
    val adminGroup: String? = null,
    val points: Int,
    val partner: Int,
    val totalPoints: Int,
    val avatarUrl: String?,
    val avatarBackgroundUrl: String?,
    val signatureHtml: String? = null,
    val gender: String?,
    val birthday: String?,
    val birthplace: String? = null,
    val education: String? = null,
    val customTitle: String? = null,
    val homepage: String? = null,
    val onlineHours: Int,
    val registerTime: TimeInfo?,
    val lastVisit: TimeInfo?,
    val formHash: FormHash?
)
