你是 Kotlin/Compose（Compose Multiplatform，支持android/ios）資深工程師。請在我的專案中製作一個「lib：PostCard 渲染器」，用於論壇 thread 畫面中渲染單一 Post（像網頁那樣一樓一樓往下）。App 層會使用 LazyColumn 列出很多 PostCard；lib 只負責單一 PostCard 的 UI + 內容 HTML 渲染 + 回報閱讀錨點，不使用 WebView。

【已存在的 DTO (commonMain/.../dto/page)（請直接使用，不要重造）】
我已經有：
data class Post(
val pid: PostId,
val floor: Int,
val author: User,
val timeText: String,
val editedText: String? = null,
val contentHtml: String,
val images: List<PostImage> = emptyList(),
val attachments: List<Attachment> = emptyList(),
val comments: List<PostComment> = emptyList(),   // 点评
val rates: List<PostRate> = emptyList()          // 评分
)

你需要讓渲染入口直接吃這個 Post DTO。

【UI 需求（對齊網頁樣式，像圖1/圖2/圖3）】
PostCard 需要包含：
1) Header（圖1）：
    - 作者 avatar（圓形）
    - 作者名
    - timeText
    - 右側：floor（例如 1#），以及 views/replies（若我 DTO 未提供，先以可選欄位/slot 支援或先不顯示）
2) Body：
    - editedText（若有，顯示在內容區上方）
    - contentHtml 渲染（message div innerHTML）
    - images/attachments（若 contentHtml 裡已含 img 可以避免重複；附件以列表顯示）
3) 点评區塊（圖2 上半）：
    - 若 comments 非空，顯示「点评」區塊
    - 每則点评顯示：user（名字/可選 avatar）、timeText、message
4) 评分區塊（圖2 下半）：
    - 顯示「评分」區塊（有 rates 才顯示）
    - 顯示參與人數（rates.size）
    - 顯示積分總和（score 總和）
    - 列出每條 rate：userName、score、reason（可空）
5) 底部按鈕（圖3）：
    - 「评分」按鈕
    - 「点评」按鈕

【互動（全部由 app 注入 callback）】
PostCard 需要暴露 callback：
- onUserClick(user: User)
- onLinkClick(href: String)
- onImageClick(url: String)
- onAttachmentClick(attachment: Attachment)
- onRateClick(post: Post) 或 onRateButtonClick(pid)
- onCommentClick(post: Post) 或 onCommentButtonClick(pid)

lib 不做任何 navigation，不存 DB，不讀 thread 狀態。

【渲染與解析核心（contentHtml）】
1) 不用 WebView。
2) HTML → Normalize → AST(Block/Span) → Compose 渲染
3) 白名單支援並可擴充：
    - 段落/換行（br、brbr 切段）
    - strong/b/i/u/s
    - a href（站內/站外連結統一回調）
    - img（支援 src/data-original 等常見變體）
    - quote（div.quote/blockquote）
    - collapse/showhide（可點擊展開）
    - code/pre
    - ul/ol/li
    - font face/size/color 與 inline style：採白名單與 featureFlags 控制（可關閉）
4) 未知 tag / 未知 style：忽略 tag、保留文字內容，絕不 throw。

【使用者樣式注入（RenderConfig）】
由 App 層傳入 config，需支援動態更新（Compose recompose）：
- textScale（字體縮放）
- lineHeightScale
- theme（背景/文字色：數種組合 必須包含Default(背景色 : #fcf4cf, 未有特殊style的文字 : #6e2b19, timeText&editText : #A6A6A6, 積分區 : 用戶名稱和理由 #999999 積分顯示 #FF5656, 點評區文字 : #333333, 詳細顏色配置如附圖(render/example中))）
- fontFamily（Sans/Serif/Mono）
- featureFlags：enablePostColors / enablePostFontSizes / enablePostFonts / enableCollapse
- anchorLineSpec（用於閱讀錨點計算）
- configVersion（app 設定變動可遞增，用於進度相容）

【閱讀進度（關鍵）】
這個 PostCard 會被放在 thread 的外層 LazyColumn 中捲動，因此 PostCard 自己不能再捲動（避免 nested scroll）。
但 lib 仍需能回報「此 PostCard 內的閱讀位置」給 App，以便 App 存 thread-level progress：(pid + anchorWithinPost)。

請實作：
- data class PostReadingAnchor(val pid: PostId, val blockIndex: Int, val intraOffset: Int = 0)
- PostCard 需要參數：
    - reportProgressForPid: PostId?（只有當 reportProgressForPid == post.pid 時才回報，避免多張卡互相打架）
    - scrollTick: Int（外層捲動時遞增，用來觸發重新計算 anchor）
    - initialAnchor: PostReadingAnchor?（用於恢復）
    - onAnchorChanged(anchor)

錨點計算必須解決「段落/圖片切半」：使用 anchorLine 覆蓋法
- 定義一條水平 anchorLine（例如距離螢幕頂 80dp，或螢幕高度的 20%）
- 每個 Block 量測 topY/bottomY（window 座標）
- 若存在 topY <= anchorLine < bottomY，該 blockIndex 就是 anchor
- 否則取 topY 與 anchorLine 距離最小者

恢復位置（initialAnchor）：
- 需提供 bringIntoView 機制：在 PostCard 內對每個 block 綁定 BringIntoViewRequester
- initialAnchor 來時，能 bring 指定 blockIndex 進視窗（一次性）
- 若 layout 尚未完成，需延後到 layout 後再執行

【模組化與交付】
請新增/調整以下檔案（路徑可依我的 repo）：
1) post-renderer lib module
2) AST 定義（Block/Span/StyleBundle）
3) Normalizer + Parser（可先 minimal 支援 br/段落/strong/a/img，後續擴充）
4) Compose renderer：PostCard（含 Header/Body/点评/评分/按鈕）
5) Progress：anchorLine 覆蓋法 + bringIntoView
6) RenderConfig + FeatureFlags
7) 測試：parser 對每個 HTML case 不崩潰；能產生 blocks；基本元素識別或降級

【非常重要：互動式收集案例】
- 請看jvmTest, 先透過fetchFavorite獲取2到3頁我的FavoriteType.Thread的收藏, 再fetchThreadById獲取這些範例Thread第一頁即可，每次fetch都要有間隔，避免給網站造成負擔
- 注意，每次獲取範例後儲存(可儲存至任意你方便讀取的地方)，取得完所有dto範例後，再繼續進行開始製作PostCard
- PostCard function命名為PostRenderer，且代碼需清晰，要有常規api級別註釋
- 所有相關代碼/class等都放到(commonMain/.../render) 資料夾，輔助function放到render/util

現在開始實作。先交付最小可用：Header + contentHtml（段落/換行/粗體/連結/圖片）+ 点评/评分區塊 + 底部按鈕 + anchorLine 進度回報 + bringIntoView 恢復 + textScale/theme 動態更新。