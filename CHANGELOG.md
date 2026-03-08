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