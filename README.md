# yamibo-api

[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Version](https://img.shields.io/badge/version-1.0.3-green.svg)]()

[English](#english-version) | [中文](#traditional-chinese-version)

---
A purely functional, highly asynchronous Kotlin Multiplatform (KMP) client library for the Yamibo forum. `yamibo-api` handles network requests, session management, and HTML parsing, translating complex forum data into highly accessible, strongly-typed Kotlin Data Transfer Objects (DTOs) for Android, iOS, and JVM.

### ✨ Features
* **Kotlin Multiplatform (KMP):** Supports Android, iOS, and JVM out of the box using Ktor.
* **Coroutines First:** All network and parsing operations are suspended, thread-safe, and asynchronous.
* **Strongly Typed DTOs:** No need to parse HTML manually. Get structured `ProfilePage`, `ThreadPage`, and `ForumPage` data immediately.
* **Safe Error Handling:** Provides a `YamiboResult` sealed class wrapping `Success`, `Failure`, `Maintenance`, and `NotLoggedIn` states.

### 🧰 Compatibility
| Dependency | Supported Version |
|---|---|
| **Kotlin** | 2.3.10+ |
| **Gradle** | 8.14.3+ |
| **Java JVM Target** | 11+ |

### 📦 Installation

#### Gradle (Kotlin DSL)
Add the dependency to your `build.gradle.kts`:

```kotlin notebook
dependencies {
    implementation("io.github.littlesurvival:yamibo-api:1.0.5")
}
```

#### Maven
Include the following in your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.littlesurvival</groupId>
    <artifactId>yamibo-api</artifactId>
    <version>1.0.5</version>
</dependency>
```

### 🚀 Quick Start

#### 1. Initialization & Authentication
The entry point of the library is the `YamiboClient`. Most Yamibo routes require authentication, so you must supply the user's cookie.

```kotlin notebook
import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.fetch.FetchFactory

// 1. Create a client instance
val client = YamiboClient(
    device = FetchFactory.Companion.Device.MOBILE, // Simulates mobile requests
    timeoutMillis = 30_000L                        // 30 seconds timeout
)

// 2. Set the user authentication cookie
client.setCookie("your_user_cookie_here")
```

#### 2. Handling API Results
All methods return a `YamiboResult<T>`, ensuring you handle all edge cases elegantly.

```kotlin notebook
import io.github.littlesurvival.core.YamiboResult

suspend fun fetchYamiboHome() {
    when (val result = client.fetchHomePage()) {
        is YamiboResult.Success -> {
            val homePage = result.value
            println("Welcome! Found ${homePage.forums.size} forums.")
        }
        is YamiboResult.NotLoggedIn -> {
            println("Error: Cookie expired or invalid.")
        }
        is YamiboResult.Maintenance -> {
            println("Yamibo is currently under maintenance.")
        }
        is YamiboResult.Failure -> {
            println("Network or parsing failed: ${result.message}")
        }
    }
}
```

#### 3. Core API Examples

**Fetching a Forum Page:**
```kotlin notebook
import io.github.littlesurvival.dto.value.ForumId

suspend fun loadForum() {
    // Fetch forum layout and threads on page 1
    val forumResult = client.fetchForumById(ForumId(5), page = 1)
}
```

**Fetching a Thread Context:**
```kotlin notebook
import io.github.littlesurvival.dto.value.ThreadId

suspend fun loadThread() {
    // Fetch posts and details within a specific thread
    val threadResult = client.fetchThreadById(ThreadId(12345), page = 1)
}
```

**Fetching User Profile:**
```kotlin notebook
suspend fun loadProfile() {
    // Requires a valid cookie to parse current user details
    val profileResult = client.fetchProfileInfo()
}
```

**Searching the Forum:**
```kotlin notebook
import io.github.littlesurvival.dto.value.FormHash

suspend fun doSearch() {
    val searchResult = client.fetchSearch(
        query = "Yuri",
        forumId = null,     // Optional: Search within a specific forum
        formHash = FormHash("your_form_hash") // Security token
    )
}
```

**Interactions (Replying, Rating, Favorites):**
```kotlin notebook
import io.github.littlesurvival.dto.value.PostId

suspend fun interact() {
    // Add thread to favorites
    client.fetchAddFavorite(ThreadId(12345), FormHash("your_form_hash"))
    
    // Reply to a thread/post
    client.fetchReplyPost(ThreadId(12345), PostId(9876), "Great post!", FormHash("your_form_hash"))
    
    // Rate a specific post
    client.fetchRatePost(ThreadId(12345), PostId(9876), score = 1, reason = "Thanks", FormHash("your_form_hash"))
}
```

---

<br>
這是一個專為 Yamibo 百合會論壇設計的 Kotlin Multiplatform (KMP) 客戶端函式庫。`yamibo-api` 處理了所有底層的網路請求、連線階段管理以及繁瑣的 HTML 結構解析，並將論壇資料轉換為強型別且易用的 Kotlin 資料傳輸物件 (DTO)，完美支援 Android、iOS 以及 JVM 平台。

### ✨ 核心特性
* **跨平台支援 (KMP)：** 透過 Ktor 網路框架，開箱即支援 Android、iOS 以及 JVM 等不同受眾。
* **協程優先 (Coroutines First)：** 所有網路與網頁解析操作皆為掛起函數 (suspend)，支援完全非同步且線程安全。
* **強型別模型 (Strongly Typed DTOs)：** 徹底告別手動解析 HTML 的痛苦。隨插即用 `ProfilePage`、`ThreadPage` 與 `ForumPage` 等豐富的結構化資料表。
* **安全的回報機制：** 提供 `YamiboResult` 封裝，妥善且優雅地處理 `Success` (成功)、`Failure` (失敗)、`Maintenance` (維護中) 以及 `NotLoggedIn` (未登入) 等各種極端狀態。

### 🧰 系統兼容性
| 依賴項目 | 支援版本 |
|---|---|
| **Kotlin** | 2.3.10+ |
| **Gradle** | 8.14.3+ |
| **Java JVM 目標** | 11+ |

### 📦 安裝方式

#### Gradle (Kotlin DSL)
請在模組內的 `build.gradle.kts` 新增依賴：

```kotlin notebook
dependencies {
    implementation("io.github.littlesurvival:yamibo-api:1.0.5")
}
```

#### Maven
如果你的專案使用 Maven 建置環境，請在 `pom.xml` 加入配置：

```xml
<dependency>
    <groupId>io.github.littlesurvival</groupId>
    <artifactId>yamibo-api</artifactId>
    <version>1.0.5</version>
</dependency>
```

### 🚀 快速開始

#### 1. 初始化與身份驗證
操作本函式庫最核心的點位就是 `YamiboClient`。由於大多數的 Yamibo 網站路由都需要論壇使用者權限，你必須利用方法設定有效的使用者 Cookie。

```kotlin notebook
import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.fetch.FetchFactory

// 1. 建立 Client 實體物件
val client = YamiboClient(
    device = FetchFactory.Companion.Device.MOBILE, // 模擬手機端請求 (此為預設值)
    timeoutMillis = 30_000L                        // 設定超時時間為 30 秒鐘
)

// 2. 寫入使用者的認證 Cookie
client.setCookie("請輸入你的_user_cookie")
```

#### 2. 處理 API 回傳結果
所有牽涉到網路的 API 方法都會回傳 `YamiboResult<T>`，這項類別會幫助你應對各種網路和未知的解析情況。

```kotlin notebook
import io.github.littlesurvival.core.YamiboResult

suspend fun fetchYamiboHome() {
    when (val result = client.fetchHomePage()) {
        is YamiboResult.Success -> {
            val homePage = result.value
            println("歡迎回來！目前論壇共有 ${homePage.forums.size} 個看板。")
        }
        is YamiboResult.NotLoggedIn -> {
            println("錯誤：Cookie 已過期或是你尚未登入。")
        }
        is YamiboResult.Maintenance -> {
            println("當前 Yamibo 正在進行系統維護中。")
        }
        is YamiboResult.Failure -> {
            println("網路連線錯誤或是資料解析異常：${result.message}")
        }
    }
}
```

#### 3. 各項 API 使用範例

**讀取特定看板 (Forum)：**
```kotlin notebook
import io.github.littlesurvival.dto.value.ForumId

suspend fun loadForum() {
    // 取得版塊編號為 5 的第一頁主題與貼文列表
    val forumResult = client.fetchForumById(ForumId(5), page = 1)
}
```

**讀取討論串內容 (Thread)：**
```kotlin notebook
import io.github.littlesurvival.dto.value.ThreadId

suspend fun loadThread() {
    // 取得特定討論串的內容與它的各樓層回覆列表
    val threadResult = client.fetchThreadById(ThreadId(12345), page = 1)
}
```

**取得當前使用者資訊 (Profile)：**
```kotlin notebook
suspend fun loadProfile() {
    // 發出請求，解析並獲得當前登入使用者的個人資訊、頭像與相關統計數據
    val profileResult = client.fetchProfileInfo()
}
```

**全站或特定看板內搜尋 (Search)：**
```kotlin notebook
import io.github.littlesurvival.dto.value.FormHash

suspend fun doSearch() {
    val searchResult = client.fetchSearch(
        query = "百合",
        forumId = null,     // 選填項目：可指定要搜尋的看板 ID
        formHash = FormHash("請填入你的_form_hash") // 必要的安全驗證碼
    )
}
```

**各類使用者網頁操作 (發出回覆、給予評分、收藏文章)：**
```kotlin notebook
import io.github.littlesurvival.dto.value.PostId

suspend fun interact() {
    // 將文章加入至我的使用者收藏內
    client.fetchAddFavorite(ThreadId(12345), FormHash("你的_form_hash"))
    
    // 對指定文章主題內進行樓層的發文回覆
    client.fetchReplyPost(ThreadId(12345), PostId(9876), "感謝大大的熱情分享！", FormHash("你的_form_hash"))
    
    // 對其他使用者的指定樓層文章進行論壇評分
    client.fetchRatePost(ThreadId(12345), PostId(9876), score = 1, reason = "感謝分享", FormHash("你的_form_hash"))
}
```
