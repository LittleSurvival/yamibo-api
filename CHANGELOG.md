# v1.0.3

Remove the hashTag"#" from SearchResult Param Tag(e.g. "#動漫區" → "動漫區")

# v1.0.4

Add new feature :
```kotlin notebook
fetchSearchById(query: String, searchId: SearchId, page: Int = 1)
```
Update SearchPage Dto :
```kotlin notebook
data class SearchPage(
    val searchId: SearchId? = null,
    val query: String,
    val threads: List<ThreadSummary>,
    val totalCount: Int,
    val pageNav: PageNav? = null,
)
```

# v1.0.5
```kotlin notebook
fetchFindPost(threadId: ThreadId? = null, authorId: UserId? = null, postId: PostId)
```
Find the location of thread page where the post id locate.
Return type is ThreadPage.

# v1.0.6
Update Parser System :
Clean up code, and rewrite the algorithm of parser, enhance the performance by 2x more efficiency and less memory cost.

Update Post DTO :
```kotlin notebook
data class Post(
    ...
    val title: String,
    ...
)
```
Add Title value for Post DTO, it parses the possibly title from the start of post content.
This feature is design for forum "文學區".

# v1.0.7
Update FavoriteItem DTO :
```kotlin notebook
data class FavoriteItem(
    val name: String,
    val url: String,
    val favId: FavoriteId
)
```
Change delete url param to favoriteId.

Add Delete and AddForum to YamiboRoute.Favorite : 
```kotlin notebook
data class Delete(val favoriteId: FavoriteId) : YamiboRoute()
 data class AddForum(val forumId: ForumId, val formHash: FormHash) : YamiboRoute()
```

Make fetchFavorite can accept forum id and thread id.
The Id type is the interface that all type-safe id implements.
```kotlin notebook
suspend fun fetchAddFavorite(id: Id, formHash: FormHash): YamiboResult<String>
```

# v1.0.8
Update Post DTO :
```kotlin notebook
data class Post(
    ...
    val poll: Poll?,
    ...
)
```
Add `poll` to `Post` to represent the poll information located at the top of the thread page. Note that polls are unique in a thread and forced to be at the first floor.

Add VotePoll feature:
```kotlin notebook
suspend fun votePoll(formHash: FormHash, forumId: ForumId, threadId: ThreadId, options: List<PollOptionId>): FetchResult<String>
```
Perform a POST request to vote in a poll. It supports selecting multiple options since `options` is a `List`.

Update ThreadSummary DTO :
```kotlin notebook
data class ThreadSummary(
    ...
    val hasPoll: Boolean,
    ...
)
```
Add `hasPoll` to `ThreadSummary` to identify if a thread contains a poll when displayed in a thread list like forum or search pages.

# v1.0.9
Old :
```html
<div id="postmessage_{id}" class="postmessage">
    {content}
</div>
```
New :
```html
{content}
```
Fix the issue that poll thread HTML content is not parsed correctly.

# 1.0.10
Add two link builder in YamiboRoute :
```kotlin notebook
//Reply a specific post(mention a post)
data class PostReply(val threadId: ThreadId, val postId: PostId, val page: Int = 1)
//Reply the thread : 
data class ThreadReply(val threadId: ThreadId, val page: Int = 1)
```

# 1.0.12
Fix PostReply build the wrong url issue.

# v1.0.13
Update Post DTO :
```kotlin notebook
data class Post(
    ...
    val tags: Tags,
    ...
)
```
Add `tags` to `Post` to represent the tag information associated with a thread. Tags are typically extracted from the first floor of a thread and are commonly used in forums like manga for cataloging.

Add Tag Search Result Parsing :
Implemented `TagPagParser` and `TagPage` DTO to support parsing for tag search result pages (e.g., `misc.php?mod=tag`).

Update ThreadSummary DTO :
```kotlin notebook
data class ThreadSummary(
    ...
    /** 
     * Forum Id (fid). 
     * @see TagPage only 
     */
    val fid: ForumId? = null,

    /** 
     * Attachment type. 
     * @see TagPage only 
     */
    val attachmentType: AttachmentType? = null,
    ...
)
```
Added `fid` and `attachmentType` to `ThreadSummary` specifically for tag search result pages, mapping common attachment icons to `Image` or `Other`.

# v1.0.14
Add Forum Type classify function in YamiboForum
```kotlin notebook
fun isNovelForum(name: String)
fun isNovelForum(forumId: ForumId)
fun isMangaForum(name: String)
fun isMangaForum(forumId: ForumId)
```

# v1.0.15
Fix the issue of cannot get image from some type of thread

# v1.0.16
Add two function in YamiboForum
```kotlin notebook
fun toForumName(forumId: ForumId): String?
fun toForumId(forumName: String): ForumId?
```

# v1.0.17
Add tagName param to TagPage.
```kotlin notebook
data class TagPage(
    val tagName: String,
    val threadSummaries : List<ThreadSummary>,
    val pageNav: PageNav? = null
)
```

# v1.0.18
Fix TagPage Link did not load page param issue.

# v1.0.19
Make all data classes @Serializable.

# v1.0.20
Fix/Add kotlin serialization plugin compilation.

# v1.0.21
Add removeFavorite feature
```kotlin notebook
suspend fun removeFavorite(favoriteId: FavoriteId): YamiboResult<String>
```
The FavoriteId can only get from FavoritePage.

# v1.0.22
Add official `kotlinx-datetime` dependency for robust date-time conversions in KMP (`commonMain`).

Introduce `TimeInfo` Model :
```kotlin notebook
data class TimeInfo(
    val text: String,
    val specialText: String? = null,
    val epoch: Long
)
```
Store time data with its computed UTC+8 epoch timestamp, raw date text, and explicit contextual text if exists (e.g., "本帖最后由...").

Update Post DTO :
```kotlin notebook
data class Post(
    ...
    val timeCreate: TimeInfo,
    val lastEditedTime: TimeInfo?,
    ...
)
```
Renamed `timeText` to `timeCreate` and `editedText` to `lastEditedTime`.

Refactor time string properties to `TimeInfo` across DTOs :
- `ThreadPage.PostComment` : `time`
- `ThreadPage.Attachment` : `timeUpload`
- `ThreadPage.Poll` : `endTime`
- `ThreadSummary` : Renamed `lastUpdateText` to `lastUpdate`
- `ProfilePage` : `registerTime`, `lastVisit`
# v1.0.23

Stop getting all /static/image URLs as images in ThreadPage

# v1.0.24

- Change NotLoggedIn Result's message to "登入狀態已失效或尚未登入，請重新登入"
- Add New URL builder "PostThread".
```kotlin notebook
data class PostThread(val forumId: ForumId)
```

# v1.1.0

Add UserSpace page parsing and fetch APIs :
```kotlin notebook
suspend fun fetchUserSpaceThreads(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadPage>
suspend fun fetchUserSpaceThreadReplies(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadReplyPage>
suspend fun fetchUserSpaceMyBlogs(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceBlogPage>
suspend fun fetchUserSpaceFriendBlogs(page: Int = 1): YamiboResult<UserSpaceBlogPage>
suspend fun fetchUserSpaceViewAllBlogs(type: YamiboRoute.UserSpace.Blog.ViewAllType, page: Int = 1): YamiboResult<UserSpaceBlogPage>
suspend fun fetchUserSpaceFriends(type: YamiboRoute.UserSpace.FriendPageType, page: Int = 1): YamiboResult<UserSpaceFriendPage>
suspend fun fetchUserSpacePrivateMessages(page: Int = 1): YamiboResult<UserSpacePrivateMessagePage>
suspend fun fetchUserSpaceNotices(page: Int = 1): YamiboResult<UserSpaceNoticePage>
```

Add RatePopout page parsing and fetch API :
```kotlin notebook
data class RatePopoutPage(
    val availableScores: List<Int>,
    val defaultReasons: List<String>,
)

suspend fun fetchRatePopoutPage(tId: ThreadId, pId: PostId): YamiboResult<RatePopoutPage>
```
When RatePopout returns a Discuz post response instead of form data, it now parses the message text as a parse failure.

Update ProfilePage DTO :
```kotlin notebook
data class ProfilePage(
    ...
    val avatarBackgroundUrl: String?,
    ...
)
```
Add `avatarBackgroundUrl` for profile avatar background image.

Add new typed ID and parse utilities for UserSpace pages :
```kotlin notebook
data class NoticeId(override val value: Long) : Id
```
Move shared thread, post, blog, notice, and user id extraction into ParseUtils.

# v1.1.1

Add BlogPage parsing and fetch API :
```kotlin notebook
data class BlogPage(
    val blogInfo: BlogInfo,
    val rootBlog: BlogComment,
    val blogComments: List<BlogComment>,
    val pageNav: PageNav? = null
)

suspend fun fetchBlogPage(blogId: BlogId, userId: UserId? = null, page: Int = 1): YamiboResult<BlogPage>
```
`rootBlog` is parsed on every blog comment page, while `blogComments` and `pageNav` follow the selected page.

Add Blog Comment feature :
```kotlin notebook
suspend fun fetchBlogComment(
    blogId: BlogId,
    userId: UserId,
    message: String,
    formHash: FormHash
): YamiboResult<String>
```
Add `YamiboRoute.BlogComment` and `BlogCommentPostFactory` for Yamibo blog quick comments.

Update UserSpaceNotice DTO :
```kotlin notebook
data class NoticeItem(
    ...
    val contentHtml: String,
    ...
)
```
Replace parsed `message`, `actor`, and `links` with raw `contentHtml`.

Update UserSpace thread/reply parsing :
- Parse `ThreadSummary.fid` from "My threads" forum links.
- Add `ReplyItem.fid` for "My replies" when the source HTML contains a forum ID.

Add BlogCommentId and shared parse utility :
```kotlin notebook
@JvmInline value class BlogCommentId(val value: Int) : Id
```
Add `ParseUtils.extractBlogCommentId(...)`.
