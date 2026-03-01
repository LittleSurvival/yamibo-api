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



