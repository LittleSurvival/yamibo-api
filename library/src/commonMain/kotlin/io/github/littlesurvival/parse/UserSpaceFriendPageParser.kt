package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.UserSpaceFriendItem
import io.github.littlesurvival.dto.page.UserSpaceFriendPage
import io.github.littlesurvival.parse.util.ParseUtils

class UserSpaceFriendPageParser : Parser<UserSpaceFriendPage> {

    override suspend fun parse(html: String): ParseResult<UserSpaceFriendPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            val activeTabText = doc.selectFirst(".dhnv a.mon")?.text()?.trim() ?: ""
            val type = when (activeTabText) {
                "在线成员", "在線成員" -> YamiboRoute.UserSpace.FriendPageType.OnlineMember
                "我的访客", "我的訪客" -> YamiboRoute.UserSpace.FriendPageType.MyVisitor
                "我的足迹", "我的足跡" -> YamiboRoute.UserSpace.FriendPageType.MyTrace
                else -> YamiboRoute.UserSpace.FriendPageType.MyFriend
            }

            val users = mutableListOf<UserSpaceFriendItem>()
            val items = doc.select("#friend_ul li")
            for (item in items) {
                val profileLink = item.selectFirst(".mimg a[href*=uid=]") ?: item.selectFirst("a[href*=uid=]") ?: continue
                val profileUrl = profileLink.attr("href")
                val uid = ParseUtils.extractUid(profileUrl) ?: continue
                val avatarUrl = profileLink.selectFirst("img")?.attr("src")?.ifEmpty { null }

                val nameLink = item.select("a[href*=uid=]").lastOrNull() ?: profileLink
                val name = nameLink.text().trim()
                if (name.isEmpty()) continue

                val pmUrl = item.selectFirst("a[href*=do=pm][href*=touid=]")?.attr("href")?.ifEmpty { null }
                val deleteUrl = item.selectFirst("a[href*=op=ignore][href*=ac=friend]")?.attr("href")?.ifEmpty { null }
                val description = item.selectFirst(".mtxt")?.text()?.trim()?.ifEmpty { null }

                users.add(
                    UserSpaceFriendItem(
                        user = User(uid = uid, name = name, avatarUrl = avatarUrl),
                        profileUrl = profileUrl,
                        pmUrl = pmUrl,
                        deleteUrl = deleteUrl,
                        description = description
                    )
                )
            }

            ParseResult.Success(
                UserSpaceFriendPage(
                    type = type,
                    users = users,
                    pageNav = ParseUtils.parsePageNav(doc)
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse user space friend page", e)
        }
    }
}
