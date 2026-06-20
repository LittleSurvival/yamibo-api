package io.github.littlesurvival

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.page.AddFriendPopoutScreen
import io.github.littlesurvival.dto.page.BlogPage
import io.github.littlesurvival.dto.page.FavoritePage
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.page.FilterType
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.page.HomePage
import io.github.littlesurvival.dto.page.OrderType
import io.github.littlesurvival.dto.page.PrivateMessagePage
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.page.RatePopoutPage
import io.github.littlesurvival.dto.page.RateResultPopoutPage
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.page.SignActionResult
import io.github.littlesurvival.dto.page.SignPage
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.page.UserSpaceBlogPage
import io.github.littlesurvival.dto.page.UserSpaceFriendPage
import io.github.littlesurvival.dto.page.UserSpaceNoticePage
import io.github.littlesurvival.dto.page.UserSpacePrivateMessagePage
import io.github.littlesurvival.dto.page.UserSpaceThreadPage
import io.github.littlesurvival.dto.page.UserSpaceThreadReplyPage
import io.github.littlesurvival.dto.page.VotersPopoutScreen
import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.Id
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.PrivateMessageId
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.post.AddFriendFactory
import io.github.littlesurvival.fetch.post.BlogCommentPostFactory
import io.github.littlesurvival.fetch.post.FavoriteFactory
import io.github.littlesurvival.fetch.post.PrivateMessageFactory
import io.github.littlesurvival.fetch.post.RateFactory
import io.github.littlesurvival.parse.RateResultPopoutPageParser
import io.github.littlesurvival.fetch.post.CommentPostFactory
import io.github.littlesurvival.fetch.post.SearchFactory
import io.github.littlesurvival.fetch.post.SignFactory
import io.github.littlesurvival.fetch.post.VotePollFactory
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import io.github.littlesurvival.parse.AddFriendPopoutScreenParser
import io.github.littlesurvival.parse.BlogPageParser
import io.github.littlesurvival.parse.FavoritePageParser
import io.github.littlesurvival.parse.ForumPageParser
import io.github.littlesurvival.parse.HomePageParser
import io.github.littlesurvival.parse.PrivateMessagePageParser
import io.github.littlesurvival.parse.ProfilePageParser
import io.github.littlesurvival.parse.RatePopoutPageParser
import io.github.littlesurvival.parse.SearchPageParser
import io.github.littlesurvival.parse.SignPageParser
import io.github.littlesurvival.parse.TagPagParser
import io.github.littlesurvival.parse.ThreadPageParser
import io.github.littlesurvival.parse.UserSpaceBlogPageParser
import io.github.littlesurvival.parse.UserSpaceFriendPageParser
import io.github.littlesurvival.parse.UserSpaceNoticePageParser
import io.github.littlesurvival.parse.UserSpacePrivateMessagePageParser
import io.github.littlesurvival.parse.UserSpaceThreadPageParser
import io.github.littlesurvival.parse.UserSpaceThreadReplyPageParser
import io.github.littlesurvival.parse.VotersPopoutScreenParser
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
    private val privateMessageFactory: PrivateMessageFactory = PrivateMessageFactory(mobileFetcher as FetchFactory)
    private val addFriendFactory: AddFriendFactory = AddFriendFactory(mobileFetcher as FetchFactory)
    private val votePollFactory: VotePollFactory = VotePollFactory(mobileFetcher as FetchFactory)
    private val signFactory: SignFactory = SignFactory(mobileFetcher as FetchFactory)

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
    private val addFriendPopoutScreenParser = AddFriendPopoutScreenParser()
    private val blogPageParser = BlogPageParser()
    private val privateMessagePageParser = PrivateMessagePageParser()
    private val ratePopoutPageParser = RatePopoutPageParser()
    private val rateResultPopoutPageParser = RateResultPopoutPageParser()
    private val votersPopoutScreenParser = VotersPopoutScreenParser()
    private val userSpaceThreadPageParser = UserSpaceThreadPageParser()
    private val userSpaceThreadReplyPageParser = UserSpaceThreadReplyPageParser()
    private val userSpaceBlogPageParser = UserSpaceBlogPageParser()
    private val userSpaceFriendPageParser = UserSpaceFriendPageParser()
    private val userSpacePrivateMessagePageParser = UserSpacePrivateMessagePageParser()
    private val userSpaceNoticePageParser = UserSpaceNoticePageParser()
    private val signPageParser = SignPageParser()

    /**
     * 抓首頁
     *
     * Fetch Yamibo home page.
     */
    suspend fun fetchHomePage(): YamiboResult<HomePage> =
        fetchAndParse(YamiboRoute.Home.build(), homePageParser)

    /**
     * 每日簽到頁面
     *
     * Fetch the daily sign-in page.
     *
     * [cookie] is an optional raw Cookie header override. Pass a merged login cookie string
     * including `cf_clearance` when the caller obtains Cloudflare cookies from WebView. If omitted,
     * the cookie previously configured with [setCookie] is used.
     */
    suspend fun fetchSignPage(cookie: String? = null): YamiboResult<SignPage> {
        cookie?.let(::setCookie)
        return when (val fetched = signFactory.fetchSignPage()) {
            is FetchResult.Success -> mapParseResult(signPageParser.parse(fetched.value), fetched.url, fetched.value)
            is FetchResult.Failure -> mapFetchFailure(fetched, fetched.url)
        }
    }

    /**
     * 執行簽到或補簽
     *
     * Execute a sign-in or repair-sign action URL parsed from [SignPage].
     *
     * [cookie] follows the same rules as [fetchSignPage].
     */
    suspend fun fetchSignAction(actionUrl: String, cookie: String? = null): YamiboResult<SignActionResult> {
        cookie?.let(::setCookie)
        return when (val fetched = signFactory.fetchSignAction(actionUrl)) {
            is FetchResult.Success -> mapParseResult(signPageParser.parseActionResult(fetched.value), fetched.url, fetched.value)
            is FetchResult.Failure -> mapFetchFailure(fetched, fetched.url)
        }
    }

    /**
     * 用戶資料頁面
     *
     * Fetch a profile page. When [userId] is null, Yamibo returns the current user's profile.
     */
    suspend fun fetchProfileInfo(userId: UserId? = null): YamiboResult<ProfilePage> =
        fetchAndParse(YamiboRoute.UserSpace.ProfileInfo(userId).build(), profilePageParser)

    /**
     * 用戶空間的主題頁面
     *
     * Fetch user-space thread list. When [userId] is null, Yamibo returns the current user's list.
     */
    suspend fun fetchUserSpaceThreads(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadPage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Thread(userId, YamiboRoute.UserSpace.ThreadType.Thread, page).build(),
            userSpaceThreadPageParser
        )

    /**
     * 用戶空間的回覆頁面
     *
     * Fetch user-space reply list. When [userId] is null, Yamibo returns the current user's list.
     */
    suspend fun fetchUserSpaceThreadReplies(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadReplyPage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Thread(userId, YamiboRoute.UserSpace.ThreadType.Reply, page).build(),
            userSpaceThreadReplyPageParser
        )

    /**
     * 用戶空間的我的日誌頁面
     *
     * Fetch user-space "my blogs" list. When [userId] is null, Yamibo returns the current user's list.
     */
    suspend fun fetchUserSpaceMyBlogs(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceBlogPage> =
        fetchAndParse(YamiboRoute.UserSpace.Blog.MyBlog(userId, page).build(), userSpaceBlogPageParser)

    /**
     * 用戶空間的好友日誌頁面
     *
     * Fetch the current user's friend-blog list.
     */
    suspend fun fetchUserSpaceFriendBlogs(page: Int = 1): YamiboResult<UserSpaceBlogPage> =
        fetchAndParse(YamiboRoute.UserSpace.Blog.FriendBlog(page).build(), userSpaceBlogPageParser)

    /**
     * 用戶空間的隨便看看日誌頁面
     *
     * Fetch public user-space blogs, either latest or hot, depending on [type].
     */
    suspend fun fetchUserSpaceViewAllBlogs(
        type: YamiboRoute.UserSpace.Blog.ViewAllType = YamiboRoute.UserSpace.Blog.ViewAllType.Latest,
        page: Int = 1
    ): YamiboResult<UserSpaceBlogPage> =
        fetchAndParse(YamiboRoute.UserSpace.Blog.ViewAll(type, page).build(), userSpaceBlogPageParser)

    /**
     * 日誌閱讀頁面
     *
     * Fetch a blog detail page and its comment page. [userId] may be omitted when the URL does not require it.
     */
    suspend fun fetchBlogPage(blogId: BlogId, userId: UserId? = null, page: Int = 1): YamiboResult<BlogPage> =
        fetchAndParse(YamiboRoute.BlogPage(blogId, userId, page).build(), blogPageParser)

    /**
     * 用戶空間的好友相關頁面
     *
     * Fetch a user-space friend page selected by [type].
     */
    suspend fun fetchUserSpaceFriends(
        type: YamiboRoute.UserSpace.FriendPageType,
        page: Int = 1
    ): YamiboResult<UserSpaceFriendPage> =
        fetchAndParse(YamiboRoute.UserSpace.MyFriend(type, page).build(), userSpaceFriendPageParser)

    /**
     * 新增好友彈窗
     *
     * Fetch the add-friend popout form for [userId].
     */
    suspend fun fetchAddFriendPopoutScreen(userId: UserId): YamiboResult<AddFriendPopoutScreen> =
        fetchAndParse(
            YamiboRoute.UserSpace.AddFriend.AddFriendPopoutPage(userId).build(),
            addFriendPopoutScreenParser
        )

    /**
     * 我的消息頁面
     *
     * Fetch the current user's private-message notification list.
     */
    suspend fun fetchUserSpacePrivateMessages(page: Int = 1): YamiboResult<UserSpacePrivateMessagePage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Notification(YamiboRoute.UserSpace.NotificationType.MyMessage, page).build(),
            userSpacePrivateMessagePageParser
        )

    /**
     * 私信聊天頁面
     *
     * Fetch a private-message conversation with [toUser]. When [page] is null, Yamibo opens the latest page.
     */
    suspend fun fetchPrivateMessagePage(toUser: UserId, page: Int? = null): YamiboResult<PrivateMessagePage> =
        fetchAndParse(YamiboRoute.PrivateMessagePage(toUser, page).build(), privateMessagePageParser)

    /**
     * 我的提醒頁面
     *
     * Fetch the current user's notice list.
     */
    suspend fun fetchUserSpaceNotices(page: Int = 1): YamiboResult<UserSpaceNoticePage> =
        fetchAndParse(
            YamiboRoute.UserSpace.Notification(YamiboRoute.UserSpace.NotificationType.MyNotice, page).build(),
            userSpaceNoticePageParser
        )

    /**
     * 論壇版塊頁面
     *
     * Fetch a forum page with optional forum filter and order parameters.
     */
    suspend fun fetchForumById(
        fId: ForumId,
        filterType: FilterType? = null,
        orderType: OrderType? = null,
        page: Int = 1
    ): YamiboResult<ForumPage> =
        fetchAndParse(YamiboRoute.Forum(fId, filterType, orderType, page).build(), forumPageParser)

    /**
     * 帖子閱讀頁面
     *
     * Fetch a thread page. [authorId] limits posts to one author, and [reverse] requests reverse order.
     */
    suspend fun fetchThreadById(
        tId: ThreadId,
        authorId: UserId? = null,
        reverse: Boolean = false,
        page: Int = 1
    ): YamiboResult<ThreadPage> =
        fetchAndParse(YamiboRoute.Thread(tId, authorId, reverse, page).build(), threadPageParser)

    /**
     * 標籤頁面
     *
     * Fetch a desktop tag page for thread results.
     */
    suspend fun fetchTagPageById(tagId: TagId, page: Int = 1): YamiboResult<TagPage> =
        fetchAndParse(YamiboRoute.TagPage(tagId, page).build(), tagPageParser, true)

    /**
     * 帖子頁面並提取標籤
     *
     * Fetch a thread page and parse only its extracted tag data.
     */
    suspend fun fetchExtractTagsInThreadById(tId: ThreadId): YamiboResult<Tags> =
        fetchAndParse(YamiboRoute.Thread(tId).build(), threadPageExtractTagParser, true)

    /**
     * findpost定位
     *
     * Fetch the thread page that contains [postId] in Yamibo's full-view findpost flow.
     */
    suspend fun fetchFindPost(threadId: ThreadId? = null, authorId: UserId? = null, postId: PostId): YamiboResult<ThreadPage> =
        fetchAndParse(YamiboRoute.FindPost(authorId, threadId, postId).build(), threadPageParser)

    /**
     * 固定常量論壇版塊頁面
     *
     * Fetch a forum page from a [YamiboForum] enum entry.
     */
    suspend fun fetchConstantForum(forum: YamiboForum, page: Int = 1): YamiboResult<ForumPage> =
        fetchAndParse(YamiboRoute.Forum(forum.forumId, page = page).build(), forumPageParser)

    /**
     * 評分彈窗頁面
     *
     * Fetch the rating popout page for a post.
     */
    suspend fun fetchRatePopoutPage(tId: ThreadId, pId: PostId): YamiboResult<RatePopoutPage> =
        fetchAndParse(YamiboRoute.RatePopout(tId, pId).build(), ratePopoutPageParser)

    /**
     * 查看全部評分清單.
     *
     * Fetch the all-ratings popup for a post.
     */
    suspend fun fetchRateResultPopoutPage(
        tId: ThreadId,
        pId: PostId
    ): YamiboResult<RateResultPopoutPage> =
        fetchAndParse(YamiboRoute.RateResultPopout(tId, pId).build(), rateResultPopoutPageParser)

    /**
     * 查看參與投票的會員.
     *
     * Fetch voters for [pollOptionId]. When omitted, Yamibo returns the first option.
     */
    suspend fun fetchViewVoters(
        tId: ThreadId,
        pollOptionId: PollOptionId? = null,
    ): YamiboResult<VotersPopoutScreen> =
        fetchAndParse(YamiboRoute.ViewVoters(tId, pollOptionId).build(), votersPopoutScreenParser)

    /**
     * 執行投票
     *
     * Vote in a thread poll with the selected option IDs and current session [formHash].
     */
    suspend fun votePoll(fId: ForumId, tId: ThreadId, pollOptionIds: List<PollOptionId>, formHash: FormHash): YamiboResult<String> {
        return when (val pollResult = votePollFactory.votePoll(formHash, fId, tId, pollOptionIds)) {
            is FetchResult.Success -> YamiboResult.Success(pollResult.value)
            is FetchResult.Failure -> mapFetchFailure(pollResult, pollResult.url)
        }
    }

    /**
     * 收藏資料夾頁面
     *
     * Fetch a favorite folder page. When [userId] is null, Yamibo returns the current user's folder.
     */
    suspend fun fetchFavoritePage(
        userId: UserId? = null,
        type: FavoriteType,
        page: Int = 1
    ): YamiboResult<FavoritePage> =
        fetchAndParse(
            YamiboRoute.Favorite.GetFolder(userId, type, page).build(),
            favoritePageParser
        )

    /**
     * 執行搜尋
     *
     * Submit a search request with [query], then fetch the redirected cached search-result page.
     */
    suspend fun fetchSearch(query: String, forumId: ForumId? = null, formHash: FormHash): YamiboResult<SearchPage> {
        return when (val linkResult = searchFactory.getCacheLink(formHash, query, forumId)) {
            is FetchResult.Success -> fetchAndParse(linkResult.value, searchPageParser)
            is FetchResult.Failure -> mapFetchFailure(linkResult, linkResult.url)
        }
    }

    /**
     * 搜尋結果頁面
     *
     * Fetch a cached search-result page by [searchId].
     */
    suspend fun fetchSearchById(query: String, searchId: SearchId, page: Int = 1): YamiboResult<SearchPage> =
        fetchAndParse(YamiboRoute.Search.BySearchId(query, searchId, page).build(), searchPageParser)

    /**
     * 新增收藏
     *
     * Add a thread or forum to favorites. [id] must be a supported favorite target ID.
     */
    suspend fun fetchAddFavorite(id: Id, formHash: FormHash, description: String = "手机收藏"): YamiboResult<String> {
        return when (val result = when (id) {
            is ThreadId -> favoriteFactory.addThread(formHash, id, description)
            is ForumId -> favoriteFactory.addForum(formHash, id)
            else -> throw IllegalArgumentException("Unknown id type: $id")
        }) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    /**
     * 移除收藏
     *
     * Remove a favorite item by favorite ID.
     */
    suspend fun fetchRemoveFavorite(id: FavoriteId, formHash: FormHash): YamiboResult<String> {
        return when (val result = favoriteFactory.removeFavorite(formHash, id)) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    /**
     * 新增好友
     *
     * Send an add-friend request to [userId]. [note] is optional and [groupId] comes from
     * [AddFriendPopoutScreen.availableOption].
     */
    suspend fun fetchAddFriend(
        userId: UserId,
        formHash: FormHash,
        note: String = "",
        groupId: Int = 1
    ): YamiboResult<String> {
        return when (val result = addFriendFactory.addFriend(formHash, userId, note, groupId)) {
            is FetchResult.Success -> YamiboResult.Success(result.value)
            is FetchResult.Failure -> mapFetchFailure(result, result.url)
        }
    }

    /**
     * 帖子評分
     *
     * Rate a post with [score] and [reason].
     */
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

    /**
     * 帖子點評
     *
     * Submit a comment to a specific post.
     */
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

    /**
     * 日誌評論
     *
     * Submit a root blog comment.
     */
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

    /**
     * 發送私信
     *
     * Send a private message in an existing private-message conversation.
     */
    suspend fun fetchSendPrivateMessage(
        privateMessageId: PrivateMessageId,
        toUser: UserId,
        message: String,
        formHash: FormHash
    ): YamiboResult<String> {
        return when (val result = privateMessageFactory.sendPrivateMessage(formHash, privateMessageId, toUser, message)) {
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

    private fun <T> mapParseResult(parsed: ParseResult<T>, url: String, body: String?): YamiboResult<T> {
        return when (parsed) {
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
                    |  body  : ${bodyPreview(body)}
                    """.trimMargin(),
                    parsed.exception
                )
            }
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
                val bodyMessage =
                    ParseUtils.parseJumpCMessage(failure.bodyPreview) ?: failure.bodyPreview
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
                            |  系統信息(您当前的访问请求当中含有非法字符，已经被系统拒绝，這很可能是登入過期/未登入導致的，請嘗試重新登入/刷新登入狀態。)
                            |  若確認登入成功/刷新登入狀態後仍無法解決，請嘗試在Github上聯繫開發者
                            """.trimMargin()
                        )
                }
                YamiboResult.Failure(
                    """
                    |[HTTP ${failure.statusCode}] 請求失敗
                    |  url    : $url
                    |  body   : $bodyMessage
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
