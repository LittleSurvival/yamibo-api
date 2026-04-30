package io.github.littlesurvival

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.page.BlogPage
import io.github.littlesurvival.dto.page.FavoritePage
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.page.HomePage
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.page.RatePopoutPage
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.page.UserSpaceBlogPage
import io.github.littlesurvival.dto.page.UserSpaceFriendPage
import io.github.littlesurvival.dto.page.UserSpaceNoticePage
import io.github.littlesurvival.dto.page.UserSpacePrivateMessagePage
import io.github.littlesurvival.dto.page.UserSpaceThreadPage
import io.github.littlesurvival.dto.page.UserSpaceThreadReplyPage
import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.Id
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.post.BlogCommentPostFactory
import io.github.littlesurvival.fetch.post.FavoriteFactory
import io.github.littlesurvival.fetch.post.RateFactory
import io.github.littlesurvival.fetch.post.CommentPostFactory
import io.github.littlesurvival.fetch.post.SearchFactory
import io.github.littlesurvival.fetch.post.VotePollFactory
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import io.github.littlesurvival.parse.BlogPageParser
import io.github.littlesurvival.parse.FavoritePageParser
import io.github.littlesurvival.parse.ForumPageParser
import io.github.littlesurvival.parse.HomePageParser
import io.github.littlesurvival.parse.ProfilePageParser
import io.github.littlesurvival.parse.RatePopoutPageParser
import io.github.littlesurvival.parse.SearchPageParser
import io.github.littlesurvival.parse.TagPagParser
import io.github.littlesurvival.parse.ThreadPageParser
import io.github.littlesurvival.parse.UserSpaceBlogPageParser
import io.github.littlesurvival.parse.UserSpaceFriendPageParser
import io.github.littlesurvival.parse.UserSpaceNoticePageParser
import io.github.littlesurvival.parse.UserSpacePrivateMessagePageParser
import io.github.littlesurvival.parse.UserSpaceThreadPageParser
import io.github.littlesurvival.parse.UserSpaceThreadReplyPageParser
import io.github.littlesurvival.parse.util.ParseUtils

class YamiboClient(
    val timeoutMillis: Long = 30_000L,
) {
    /** Fetcher */
    private val mobileFetcher: Fetcher<String> = FetchFactory(FetchFactory.Device.MOBILE, timeoutMillis)
    private val desktopFetcher: Fetcher<String> = FetchFactory(FetchFactory.Device.DESKTOP, timeoutMillis)
    private val searchFactory: SearchFactory = SearchFactory(mobileFetcher as FetchFactory)
    private val favoriteFactory: FavoriteFactory = FavoriteFactory(mobileFetcher as FetchFactory)
    private val rateFactory: RateFactory = RateFactory(mobileFetcher as FetchFactory)
    private val commentPostFactory: CommentPostFactory = CommentPostFactory(mobileFetcher as FetchFactory)
    private val blogCommentPostFactory: BlogCommentPostFactory = BlogCommentPostFactory(mobileFetcher as FetchFactory)
    private val votePollFactory: VotePollFactory = VotePollFactory(mobileFetcher as FetchFactory)

    /** Initialize Values */
    fun setCookie(cookie: String) {
        mobileFetcher.setCookies(cookie)
        desktopFetcher.setCookies(cookie)
    }

    /** Parser */
    private val homePageParser = HomePageParser()
    private val profilePageParser = ProfilePageParser()
    private val forumPageParser = ForumPageParser()
    private val threadPageParser = ThreadPageParser()
    private val threadPageExtractTagParser = ThreadPageParser.ParseTag()
    private val tagPageParser = TagPagParser()
    private val searchPageParser = SearchPageParser()
    private val favoritePageParser = FavoritePageParser()
    private val blogPageParser = BlogPageParser()
    private val ratePopoutPageParser = RatePopoutPageParser()
    private val userSpaceThreadPageParser = UserSpaceThreadPageParser()
    private val userSpaceThreadReplyPageParser = UserSpaceThreadReplyPageParser()
    private val userSpaceBlogPageParser = UserSpaceBlogPageParser()
    private val userSpaceFriendPageParser = UserSpaceFriendPageParser()
    private val userSpacePrivateMessagePageParser = UserSpacePrivateMessagePageParser()
    private val userSpaceNoticePageParser = UserSpaceNoticePageParser()

    /** Fetch Pages */
    suspend fun fetchHomePage(): YamiboResult<HomePage> =
        fetchAndParse(YamiboRoute.Home.build(), homePageParser)

    suspend fun fetchProfileInfo(userId: UserId? = null): YamiboResult<ProfilePage> =
        fetchAndParse(YamiboRoute.UserSpace.ProfileInfo(userId).build(), profilePageParser)

    suspend fun fetchUserSpaceThreads(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadPage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Thread(userId, YamiboRoute.UserSpace.ThreadType.Thread, page).build(),
            userSpaceThreadPageParser
        )

    suspend fun fetchUserSpaceThreadReplies(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadReplyPage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Thread(userId, YamiboRoute.UserSpace.ThreadType.Reply, page).build(),
            userSpaceThreadReplyPageParser
        )

    suspend fun fetchUserSpaceMyBlogs(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceBlogPage> =
        fetchAndParse(YamiboRoute.UserSpace.Blog.MyBlog(userId, page).build(), userSpaceBlogPageParser)

    suspend fun fetchUserSpaceFriendBlogs(page: Int = 1): YamiboResult<UserSpaceBlogPage> =
        fetchAndParse(YamiboRoute.UserSpace.Blog.FriendBlog(page).build(), userSpaceBlogPageParser)

    suspend fun fetchUserSpaceViewAllBlogs(
        type: YamiboRoute.UserSpace.Blog.ViewAllType = YamiboRoute.UserSpace.Blog.ViewAllType.Latest,
        page: Int = 1
    ): YamiboResult<UserSpaceBlogPage> =
        fetchAndParse(YamiboRoute.UserSpace.Blog.ViewAll(type, page).build(), userSpaceBlogPageParser)

    suspend fun fetchBlogPage(blogId: BlogId, userId: UserId? = null, page: Int = 1): YamiboResult<BlogPage> =
        fetchAndParse(YamiboRoute.BlogPage(blogId, userId, page).build(), blogPageParser)

    suspend fun fetchUserSpaceFriends(
        type: YamiboRoute.UserSpace.FriendPageType,
        page: Int = 1
    ): YamiboResult<UserSpaceFriendPage> =
        fetchAndParse(YamiboRoute.UserSpace.MyFriend(type, page).build(), userSpaceFriendPageParser)

    suspend fun fetchUserSpacePrivateMessages(page: Int = 1): YamiboResult<UserSpacePrivateMessagePage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Notification(YamiboRoute.UserSpace.NotificationType.MyMessage, page).build(),
            userSpacePrivateMessagePageParser
        )

    suspend fun fetchUserSpaceNotices(page: Int = 1): YamiboResult<UserSpaceNoticePage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Notification(YamiboRoute.UserSpace.NotificationType.MyNotice, page).build(),
            userSpaceNoticePageParser
        )

    suspend fun fetchForumById(fId: ForumId, page: Int = 1): YamiboResult<ForumPage> =
        fetchAndParse(YamiboRoute.Forum(fId, page).build(), forumPageParser)

    suspend fun fetchThreadById(tId: ThreadId, authorId: UserId? = null, page: Int = 1): YamiboResult<ThreadPage> =
        fetchAndParse(YamiboRoute.Thread(tId, authorId,page).build(), threadPageParser)

    suspend fun fetchTagPageById(tagId: TagId, page: Int = 1): YamiboResult<TagPage> =
        fetchAndParse(YamiboRoute.TagPage(tagId, page).build(), tagPageParser, true)

    suspend fun fetchExtractTagsInThreadById(tId: ThreadId): YamiboResult<Tags> =
        fetchAndParse(YamiboRoute.Thread(tId).build(), threadPageExtractTagParser, true)

    suspend fun fetchFindPost(threadId: ThreadId? = null, authorId: UserId? = null, postId: PostId): YamiboResult<ThreadPage> =
        fetchAndParse(YamiboRoute.FindPost(authorId, threadId, postId).build(), threadPageParser)

    suspend fun fetchConstantForum(forum: YamiboForum, page: Int = 1): YamiboResult<ForumPage> =
        fetchAndParse(YamiboRoute.Forum(forum.forumId, page).build(), forumPageParser)

    suspend fun fetchRatePopoutPage(tId: ThreadId, pId: PostId): YamiboResult<RatePopoutPage> =
        fetchAndParse(YamiboRoute.RatePopout(tId, pId).build(), ratePopoutPageParser)

    suspend fun votePoll(fId: ForumId, tId: ThreadId, pollOptionIds: List<PollOptionId>, formHash: FormHash): YamiboResult<String> {
        return when (val pollResult = votePollFactory.votePoll(formHash, fId, tId, pollOptionIds)) {
            is FetchResult.Success -> YamiboResult.Success(pollResult.value)
            is FetchResult.Failure -> mapFetchFailure(pollResult, pollResult.url)
        }
    }
    suspend fun fetchFavoritePage(
        userId: UserId? = null,
        type: FavoriteType,
        page: Int = 1
    ): YamiboResult<FavoritePage> =
        fetchAndParse(
            YamiboRoute.Favorite.GetFolder(userId, type, page).build(),
            favoritePageParser
        )

    suspend fun fetchSearch(query: String, forumId: ForumId? = null, formHash: FormHash): YamiboResult<SearchPage> {
        return when (val linkResult = searchFactory.getCacheLink(formHash, query, forumId)) {
            is FetchResult.Success -> fetchAndParse(linkResult.value, searchPageParser)
            is FetchResult.Failure -> mapFetchFailure(linkResult, linkResult.url)
        }
    }

    suspend fun fetchSearchById(query: String, searchId: SearchId, page: Int = 1): YamiboResult<SearchPage> =
        fetchAndParse(YamiboRoute.Search.BySearchId(query, searchId, page).build(), searchPageParser)

    suspend fun fetchAddFavorite(id: Id, formHash: FormHash, description: String = "手机收藏"): YamiboResult<String> {
        return when (val result = when(id) {
            is ThreadId -> favoriteFactory.addThread(formHash, id, description)
            is ForumId -> favoriteFactory.addForum(formHash, id)
            else -> throw IllegalArgumentException("Unknown id type: $id")
        }) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    suspend fun fetchRemoveFavorite(id: FavoriteId, formHash: FormHash): YamiboResult<String> {
        return when(val result = favoriteFactory.removeFavorite(formHash, id)) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    suspend fun fetchRatePost(
        tId: ThreadId,
        pId: PostId,
        score: Int,
        reason: String,
        formHash: FormHash,
        noticeAuthor: Boolean = false,
    ): YamiboResult<String> {
        return when (val result = rateFactory.addRate(formHash, tId, pId, score, reason, noticeAuthor)) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    suspend fun fetchCommentPost(
        tId: ThreadId,
        pId: PostId,
        message: String,
        formHash: FormHash
    ): YamiboResult<String> {
        return when (val result = commentPostFactory.commentPost(formHash, tId, pId, message)) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    suspend fun fetchBlogComment(
        blogId: BlogId,
        userId: UserId,
        message: String,
        formHash: FormHash
    ): YamiboResult<String> {
        return when (val result = blogCommentPostFactory.commentBlog(formHash, blogId, userId, message)) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    /** Core fetch-and-parse pipeline. */
    private suspend fun <T> fetchAndParse(url: String, parser: Parser<T>, desktop: Boolean = false): YamiboResult<T> {
        val fetcher = if (desktop) desktopFetcher else mobileFetcher
        return when (val fetched = fetcher.getResult(url)) {
            is FetchResult.Success -> {
                when (val parsed = parser.parse(fetched.value)) {
                    is ParseResult.Success -> YamiboResult.Success(parsed.value)
                    is ParseResult.NotLoggedIn -> YamiboResult.NotLoggedIn
                    is ParseResult.Maintenance -> YamiboResult.Maintenance
                    is ParseResult.NoPermission -> YamiboResult.NoPermission(parsed.reason)
                    is ParseResult.Failure -> {
                        val errorLine = parsed.exception?.let { "\n  error : $it" } ?: ""
                        YamiboResult.Failure(
                            """
                            |[Parse] 解析失敗
                            |  url   : $url
                            |  reason: ${parsed.reason}$errorLine
                            |  body  : ${bodyPreview(fetched.value)}
                            """.trimMargin(),
                            parsed.exception
                        )
                    }
                }
            }

            is FetchResult.Failure -> mapFetchFailure(fetched, url)
        }
    }

    /**
     * Convert a [FetchResult.Failure] into a [YamiboResult.Failure].
     *
     * @param failure The fetch failure to convert.
     * @param url URL or operation name.
     */
    private fun mapFetchFailure(failure: FetchResult.Failure, url: String): YamiboResult<Nothing> {
        return when (failure) {
            is FetchResult.Failure.HttpError -> {
                /** HTTP 503 means the server is under maintenance. */
                if (failure.statusCode == 503) {
                    if (ParseUtils.isMaintenance(failure.bodyPreview))
                        return YamiboResult.Maintenance
                    if (PostResponseUtils.isIllegal(failure.bodyPreview))
                        return YamiboResult.Failure(
                            """
                            |[HTTP ${failure.statusCode}] 請求失敗
                            |  url    : $url
                            |  body   : 
                            |  系統信息(您当前的访问请求当中含有非法字符，已经被系统拒绝，這很可能是登入過期/未登入導致的，請嘗試重新登入。)
                            |  若確認登入成功後仍無法解決，請嘗試在Github上聯繫開發者
                            """.trimMargin()
                        )
                }
                YamiboResult.Failure(
                    """
                    |[HTTP ${failure.statusCode}] 請求失敗
                    |  url    : $url
                    |  body   : ${(failure.bodyPreview)}
                    """.trimMargin()
                )
            }

            is FetchResult.Failure.NetworkError ->
                YamiboResult.Failure(
                    """
                |[Network] 網路錯誤
                |  url    : $url
                |  error  : ${failure.exception.message ?: failure.exception}
                """.trimMargin(),
                    failure.exception
                )

            is FetchResult.Failure.Timeout ->
                YamiboResult.Failure(
                    """
                |[Timeout] 請求逾時 (${timeoutMillis}ms)
                |  url    : $url
                |  error  : ${failure.exception.message ?: failure.exception}
                """.trimMargin(),
                    failure.exception
                )

            is FetchResult.Failure.Unknown ->
                YamiboResult.Failure(
                    """
                |[Unknown] 未知錯誤
                |  url    : $url
                |  error  : ${failure.exception.message ?: failure.exception}
                """.trimMargin(),
                    failure.exception
                )
        }
    }

    // visual helper code.

    companion object {
        private const val BODY_PREVIEW_LIMIT = 300

        /**
         * Truncate a body string for error logging. If the body is <= [BODY_PREVIEW_LIMIT] chars,
         * return it inline. Otherwise, return the first [BODY_PREVIEW_LIMIT] chars + "...(<N>
         * lines)" so it reads like a decent IDE error log.
         */
        internal fun bodyPreview(body: String?): String {
            if (body.isNullOrBlank()) return "(empty)"
            if (body.length <= BODY_PREVIEW_LIMIT) return body

            val totalLines = body.lines().size
            // val truncated = body.take(BODY_PREVIEW_LIMIT).replace("\n", "\\n")
            return "${body.take(BODY_PREVIEW_LIMIT)} ...($totalLines lines)"
        }
    }
}
