package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils

class ProfilePageParser : Parser<ProfilePage> {

    override suspend fun parse(html: String): ParseResult<ProfilePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn

            // --- Basic Info from Header ---
            val avtNavMenu = doc.select("#avtnav_menu")
            val userLink = avtNavMenu.select("a[href*=uid]").firstOrNull()
            val uid = userLink?.attr("href")?.let { ParseUtils.extractUid(it) } ?: UserId(0)
            val username = userLink?.text()?.trim() ?: ""

            val userGroup = avtNavMenu.select("a[href*=usergroup] font").text().trim()

            // --- Avatar Transformation ---
            val avatarUrl =
                    doc.select(".user_avatar")
                            .attr("src")
                            .substringBefore("?")
                            .replace("_small.jpg", "_big.jpg")
                            .replace("_middle.jpg", "_big.jpg")
                            .ifEmpty { null }

            // --- Form Hash ---
            val formHash =
                    doc.select("input[name=formhash]").attr("value").ifEmpty { null }?.let {
                        FormHash(it)
                    }

            // --- Credits / Points ---
            val creditList = doc.select(".creditl li")
            var points = 0
            var partner = 0
            var totalPoints = 0

            for (li in creditList) {
                val text = li.text()
                val value = li.ownText().trim().removeSuffix("点").trim().toIntOrNull() ?: 0

                when {
                    text.contains("总积分") || text.contains("總積分") -> totalPoints = value
                    text.contains("对象") || text.contains("對象") -> partner = value
                    text.contains("积分") || text.contains("積分") -> points = value
                }
            }

            ParseResult.Success(
                    ProfilePage(
                            uid = uid,
                            username = username,
                            userGroup = userGroup,
                            points = points,
                            partner = partner,
                            totalPoints = totalPoints,
                            avatarUrl = avatarUrl,
                            formHash = formHash
                    )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse profile page", e)
        }
    }
}
