package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.*
import io.github.littlesurvival.dto.page.*
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils

class ThreadPageParser : Parser<ThreadPage> {

    override suspend fun parse(html: String): ParseResult<ThreadPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn

            // --- Thread info ---
            val viewTit = doc.select(".view_tit").first()
            val categoryTag = viewTit?.select("em")?.text()?.trim()?.ifEmpty { null }
            val fullTitle = viewTit?.text()?.trim() ?: ""
            val title =
                if (categoryTag != null) {
                    fullTitle.removePrefix(categoryTag).trim()
                } else {
                    fullTitle
                }

            val canonicalUrl = doc.select("link[rel=canonical]").attr("href")
            val tid =
                ParseUtils.extractTid(canonicalUrl)
                    ?: doc.select(".plc")
                        .first()
                        ?.attr("id")
                        ?.removePrefix("pid")
                        ?.toIntOrNull()
                        ?.let { ThreadId(it) }
                    ?: ThreadId(0)

            val forumLink = doc.select(".header h2 a").first()
            val forumName = forumLink?.text()?.trim() ?: ""
            val forumUrl = forumLink?.attr("href") ?: ""
            val forumFid = ParseUtils.extractFid(forumUrl) ?: ForumId(0)

            // --- Total views and replies (from first post's metadata) ---
            val firstPostMtime = doc.select(".plc .mtime .y").first()
            val viewReplyEms = firstPostMtime?.select("em")
            val totalViews = viewReplyEms?.getOrNull(0)?.text()?.trim()?.toIntOrNull()
            val totalReplies = viewReplyEms?.getOrNull(1)?.text()?.trim()?.toIntOrNull()

            // --- Reply URL ---
            val replyLinkEl = doc.select("a.viewt-reply").first()
            val replyUrl = replyLinkEl?.attr("href")?.ifEmpty { null }

            val threadInfo =
                ThreadInfo(
                    tid = tid,
                    title = title,
                    forum = ForumSummary(fid = forumFid, name = forumName, url = forumUrl),
                    categoryTag = categoryTag,
                    totalReplies = totalReplies,
                    totalViews = totalViews,
                    replyUrl = replyUrl
                )

            // --- Posts ---
            val posts = mutableListOf<Post>()
            val postEls = doc.select(".plc")
            for (postEl in postEls) {
                val pidStr = postEl.attr("id").removePrefix("pid")
                val pid = PostId(pidStr.toIntOrNull() ?: continue)

                val floorText = postEl.select(".authi .mtit .y").text().trim()
                val floor =
                    floorText.replace("#", "").replace("\\s".toRegex(), "").toIntOrNull()
                        ?: continue

                val authorEl = postEl.select(".authi .z a").first()
                val authorUid =
                    authorEl?.attr("href")?.let { ParseUtils.extractUid(it) } ?: UserId(0)
                val authorName = authorEl?.text()?.trim() ?: ""
                val avatarUrl = postEl.select(".avatar img").attr("src").ifEmpty { null }
                val author = User(uid = authorUid, name = authorName, avatarUrl = avatarUrl)

                val timeEl = postEl.select(".authi .mtime").first()
                val timeText = timeEl?.ownText()?.trim() ?: timeEl?.text()?.trim() ?: ""

                val pstatus = postEl.select(".message .pstatus").first()
                val editedText = pstatus?.text()?.trim()?.ifEmpty { null }

                val messageEl = postEl.select(".message").first()
                val contentHtml =
                    if (messageEl != null) {
                        val clone = messageEl.clone()
                        clone.select(".pstatus").remove()
                        clone.html().trim()
                    } else {
                        ""
                    }

                val images = mutableListOf<PostImage>()
                val imgEls = messageEl?.select("img") ?: emptyList()
                for (img in imgEls) {
                    val src = img.attr("src")
                    if (src.isEmpty()) continue
                    if (src.contains("static/image/smiley") || src.contains("static/image/common")
                    ) {
                        continue
                    }
                    val alt = img.attr("alt").ifEmpty { null }
                    images.add(PostImage(url = src, alt = alt))
                }

                // --- Comments (点评) for this post ---
                val comments = mutableListOf<PostComment>()
                val commentContainer = doc.select("#comment_$pidStr").first()
                if (commentContainer != null) {
                    val commentEls = commentContainer.select("[id^=commentdetail_]")
                    for (commentEl in commentEls) {
                        val commentAuthorEl = commentEl.select(".authi .z a").first()
                        val commentAuthorName = commentAuthorEl?.text()?.trim() ?: ""
                        val commentAuthorUid =
                            commentAuthorEl?.attr("href")?.let { ParseUtils.extractUid(it) }
                                ?: UserId(0)
                        val commentAvatarUrl =
                            commentEl.select(".avatar img, .user_avatar").attr("src").ifEmpty {
                                null
                            }
                        val commentUser =
                            User(
                                uid = commentAuthorUid,
                                name = commentAuthorName,
                                avatarUrl = commentAvatarUrl
                            )
                        val commentTimeText = commentEl.select(".mtime").text().trim()
                        val commentMessage = commentEl.select(".mtxt").text().trim()
                        if (commentMessage.isNotEmpty()) {
                            comments.add(
                                PostComment(
                                    user = commentUser,
                                    timeText = commentTimeText,
                                    message = commentMessage
                                )
                            )
                        }
                    }
                }

                // --- Rates (评分) for this post ---
                val rates = mutableListOf<PostRate>()
                val rateContainer = doc.select("#ratelog_$pidStr").first()
                if (rateContainer != null) {
                    val rateItems = rateContainer.select("li.flex-box.mli.p0")
                    for (rateItem in rateItems) {
                        // Skip header row (contains 参与人数) and footer row (contains 查看全部评分)
                        val headerCheck = rateItem.select(".xw1").text()
                        if (headerCheck.contains("参与人数") || headerCheck.contains("理由")) continue
                        if (rateItem.select(".dialog").isNotEmpty()) continue

                        val rateUserName =
                            rateItem.select(".flex-2 a").first()?.text()?.trim() ?: continue
                        val rateScore =
                            rateItem.select(".xi1").text().trim().ifEmpty {
                                rateItem.select(".flex-2.xs1.xi1").text().trim()
                            }
                        val rateReason = rateItem.select(".flex-3").text().trim().ifEmpty { "" }
                        rates.add(
                            PostRate(
                                userName = rateUserName,
                                score = rateScore,
                                reason = rateReason
                            )
                        )
                    }
                }

                posts.add(
                    Post(
                        pid = pid,
                        floor = floor,
                        author = author,
                        timeText = timeText,
                        editedText = editedText,
                        contentHtml = contentHtml,
                        images = images,
                        comments = comments,
                        rates = rates
                    )
                )
            }

            // --- Pagination ---
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(ThreadPage(thread = threadInfo, posts = posts, pageNav = pageNav))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse thread page", e)
        }
    }
}
