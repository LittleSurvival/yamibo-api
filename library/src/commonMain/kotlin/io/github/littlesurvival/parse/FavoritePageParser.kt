package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.FavoriteItem
import io.github.littlesurvival.dto.page.FavoritePage
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.parse.util.ParseUtils

class FavoritePageParser : Parser<FavoritePage> {

    override suspend fun parse(html: String): ParseResult<FavoritePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn

            // --- Favorite type ---
            // The active tab has class "mon"; its text maps to a FavoriteType.
            val activeTab = doc.select("#dhnav_li a.mon").first()
            val activeTabText = activeTab?.text()?.trim() ?: ""
            val type =
                    FavoriteType.entries.firstOrNull { it.typeName == activeTabText }
                            ?: FavoriteType.Thread // default

            // --- Favorite items ---
            val items = mutableListOf<FavoriteItem>()
            val itemEls = doc.select(".findbox ul li.sclist")
            for (li in itemEls) {
                // Delete link
                val deleteLink = li.select("a.mdel").first() ?: continue
                val deleteUrl = deleteLink.attr("href")

                // Content link (the second <a>, not the delete one)
                val contentLink = li.select("a:not(.mdel)").first() ?: continue
                val url = contentLink.attr("href")
                val name = contentLink.text().trim()

                items.add(FavoriteItem(name = name, url = url, deleteUrl = deleteUrl))
            }

            // --- Pagination ---
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(FavoritePage(type = type, items = items, pageNav = pageNav))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse favorite page", e)
        }
    }
}
