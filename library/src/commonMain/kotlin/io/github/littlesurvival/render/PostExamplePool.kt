package io.github.littlesurvival.render

import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.Attachment
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.PostComment
import io.github.littlesurvival.dto.page.PostRate
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.UserId

/**
 * A pool of realistic sample [Post] objects built from real Yamibo forum data.
 *
 * Useful for:
 * - IntelliJ IDEA Compose `@Preview` composables
 * - Unit / integration testing
 * - Screenshot testing
 *
 * Every post is a self-contained, hardcoded snapshot — no network access or file I/O is needed.
 */
object PostExamplePool {

    // ── Authors ──────────────────────────────────────────────────────

    private val authorDpm1212 =
        User(
            uid = UserId(410275),
            name = "dpm1212",
            avatarUrl =
                "https://bbs.yamibo.com/uc_server/data/avatar/000/41/02/75_avatar_small.jpg"
        )

    private val authorMuyuKesan =
        User(
            uid = UserId(680053),
            name = "暮雨客散",
            avatarUrl =
                "https://bbs.yamibo.com/uc_server/data/avatar/000/68/00/53_avatar_small.jpg"
        )

    private val authorMeichian =
        User(
            uid = UserId(177116),
            name = "梅奇安",
            avatarUrl =
                "https://bbs.yamibo.com/uc_server/data/avatar/000/17/71/16_avatar_small.jpg"
        )

    private val authorQihai =
        User(
            uid = UserId(177116),
            name = "七海卫风",
            avatarUrl =
                "https://bbs.yamibo.com/uc_server/data/avatar/000/17/71/16_avatar_small.jpg"
        )

    // ── Sample Posts ─────────────────────────────────────────────────

    /**
     * A simple post with basic formatted text, link, and ratings.
     *
     * Based on thread 565238 floor 1 — a light novel translation post.
     */
    val simpleWithRates =
        Post(
            pid = PostId(41426069),
            floor = 1,
            author = authorDpm1212,
            timeText = "2025-12-16 17:11",
            editedText = "本帖最后由 dpm1212 于 2025-12-27 19:49 编辑",
            contentHtml =
                """
            <br>
            <br>
            <strong><font size="4">原文链接：<a href="https://kakuyomu.jp/works/16818093088297901404" target="_blank">カクヨム</a></font></strong>
            <br>
            <br>
            <font size="4">作者：もんすたー</font>
            <br>
            <br>
            <div align="left">是非常纯爱的一本，风格有点类似我之前翻的不小心和女生接吻了。</div>
            <br>
            <div align="left">以上，祝阅读愉快。</div>
        """.trimIndent(),
            rates =
                listOf(
                    PostRate(userName = "thenano", score = 5, reason = null),
                    PostRate(userName = "堀田薰", score = 15, reason = null),
                    PostRate(userName = "dewalrer123", score = 5, reason = null),
                    PostRate(userName = "谁说不下雨", score = 5, reason = null),
                    PostRate(
                        userName = "kanvta",
                        score = 10,
                        reason = "很喜欢这种安静又顺遂的感情，感谢翻译."
                    ),
                    PostRate(userName = "musicbox233", score = 5, reason = null),
                    PostRate(
                        userName = "narziss20201",
                        score = 10,
                        reason = "你太可爱"
                    ),
                )
        )

    /**
     * A post with bold/italic/colored text and font sizes.
     *
     * Based on thread 563363 floor 1 — another translation post with rich formatting.
     */
    val richFormatting =
        Post(
            pid = PostId(41364643),
            floor = 1,
            author = authorMuyuKesan,
            timeText = "2025-10-25 15:18",
            editedText = "本帖最后由 暮雨客散 于 2026-2-17 15:41 编辑",
            contentHtml =
                """
            <br>
            <br>
            作者：深水紅茶（リプトン）
            <br>
            原文：<a href="https://kakuyomu.jp/works/16818093093165957353" target="_blank">カクヨム</a>
            <br>
            <br>
            <font color="#8B4513"><strong>【简介】</strong></font>
            <br>
            名门贵族千金・<font color="#FF0000"><b>米莉亚</b></font>，就读于王立魔法学校、奇幻风、天才×秀才、劲敌百合、异世界
            <br>
            <br>
            <font color="#8B4513"><strong>【正文】</strong></font>
            <br>
            <a href="https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&pid=41364644&ptid=563363" target="_blank">第1话 决斗</a>
            <br>
            <a href="https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&pid=41364645&ptid=563363" target="_blank">第2话 再战</a>
            <br>
            <a href="https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&pid=41364646&ptid=563363" target="_blank">第3话 邀请</a>
            <br>
            <br>
            但是不知为何，曾经毫无破绽的对手，不断地露出了破绽——
            <br>
            下一次绝对不会输了。
        """.trimIndent(),
            rates =
                listOf(
                    PostRate(userName = "yh137000", score = 1, reason = null),
                    PostRate(userName = "trjh32", score = 10, reason = null),
                    PostRate(userName = "buqimingzi", score = 5, reason = "精品文章"),
                    PostRate(userName = "hesitae", score = 1, reason = null),
                    PostRate(userName = "tear095", score = 15, reason = null),
                    PostRate(userName = "ネムちゃん", score = 15, reason = "精品文章"),
                    PostRate(
                        userName = "a0916676138",
                        score = 5,
                        reason = "好萌好萌好萌"
                    ),
                )
        )

    /**
     * A post with comments (点评) section.
     *
     * Synthetic example inspired by the forum screenshots.
     */
    val withComments =
        Post(
            pid = PostId(39476862),
            floor = 1,
            author = authorMeichian,
            timeText = "2022-10-7 22:35",
            editedText = "本帖最后由 meichian 于 2025-12-14 18：14 编辑",
            contentHtml =
                """
            <font color="#6E2B19">
            似乎在各个游戏中，恶役千金的结局都差不多是一样的吧。
            <br>
            剥夺地位，处死，流放。正因如此，若是真有人穿越成了这种角色，大约最首先想到的也是避开自己毁灭的未来。
            <br>
            <br>
            <b>但是——</b>
            <br>
            <br>
            雷娅是一位重度百合控，因 18+遊戲裡自己最為喜歡的一對 CP 未能結婚耿耿於懷，巧合之下她轉生進入了這款自己生前最為喜歡的 18+百合遊戲《霧中的箱庭——浮世之花》。
            <br>
            在那裡，她成為了拉若塔·耶·康斯依索爾，遊戲中唯一沒有 cp 的重要角色。
            <br>
            毫無疑問 CP happy end 的盡頭是自己的 bad end，自己絕不能在此斷送性命。
            <br>
            但是，如果沒有惡役千金從中作梗，主角 CP 又如何相知相愛！
            <br>
            <br>
            最喜歡的 CP 能否成婚？ 世界的未來能否被拯救？
            <br>
            <br>
            <i>以及全新人生中的惡役大小姐，又會走向怎樣的結局？</i>
            </font>
        """.trimIndent(),
            comments =
                listOf(
                    PostComment(
                        user = User(UserId(680053), "meichian", null),
                        timeText = "2022-10-7 22:41",
                        message = "因为最近患病，所以突发奇想，病人就该写点有病的东西，于是就有了这个（摊手）"
                    ),
                    PostComment(
                        user = User(UserId(100002), "读者酱", null),
                        timeText = "2022-10-8 10:15",
                        message = "太好看了！期待后续更新！"
                    ),
                ),
            rates =
                listOf(
                    PostRate(userName = "砂糖味yarrow", score = 3, reason = null),
                    PostRate(userName = "夜、羽", score = 3, reason = "精品文章"),
                    PostRate(userName = "hdgk", score = 5, reason = "精品文章"),
                    PostRate(userName = "chugox", score = 5, reason = "好耶"),
                    PostRate(userName = "咕哒子鸭", score = 5, reason = null),
                    PostRate(userName = "墨玉魂", score = 1, reason = "原创内容"),
                    PostRate(userName = "annaanastasia", score = 5, reason = null),
                    PostRate(userName = "violet99", score = 10, reason = "精品文章"),
                    PostRate(userName = "wanlau", score = 9, reason = null),
                    PostRate(userName = "xfh2023", score = 3, reason = null),
                )
        )

    /** A short reply post with a blockquote. */
    val replyWithQuote =
        Post(
            pid = PostId(39476916),
            floor = 14,
            author = authorQihai,
            timeText = "2020-3-12 19:35",
            editedText = "本帖最后由 七海卫风 于 2020-3-12 19:43 编辑",
            contentHtml =
                """
            <br>
            <br>
            名为争称号，实为争攻受
            <br>
            话说有原文网址吗？？迫不及待想去啃生肉了
        """.trimIndent()
        )

    /**
     * A post with attachments.
     *
     * Synthetic example to demonstrate the attachment section.
     */
    val withAttachments =
        Post(
            pid = PostId(99990001),
            floor = 5,
            author = User(UserId(12345), "翻译君", null),
            timeText = "2026-1-10 21:08",
            contentHtml =
                """
            <br>
            本章的翻译文件如下，请下载阅读：
            <br>
            <br>
            <b>注意：</b>本文件仅供学习交流使用。
        """.trimIndent(),
            attachments =
                listOf(
                    Attachment(
                        name = "第12话_翻译.txt",
                        url = "forum.php?mod=attachment&aid=MTIzNDU=",
                        timeUpload = "2026-1-10 21:08",
                        fileSize = "17.93 KB",
                        downloadTimes = 122
                    ),
                    Attachment(
                        name = "插图合集.zip",
                        url = "forum.php?mod=attachment&aid=NjU0MzI=",
                        timeUpload = "2026-1-10 21:09",
                        fileSize = "3.45 MB",
                        downloadTimes = 89
                    ),
                )
        )

    /** A minimal post with only plain text, no formatting, no rates, no comments. */
    val minimal =
        Post(
            pid = PostId(99990002),
            floor = 2,
            author = User(UserId(99999), "路人甲", null),
            timeText = "2026-2-1 08:30",
            contentHtml = "感谢翻译！辛苦了！"
        )

    /** All sample posts in a list — useful for LazyColumn previews. */
    val all: List<Post> =
        listOf(
            simpleWithRates,
            richFormatting,
            withComments,
            replyWithQuote,
            withAttachments,
            minimal
        )
}
