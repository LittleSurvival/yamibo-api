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