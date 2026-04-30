package io.github.littlesurvival

import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.ktor.http.*

sealed class YamiboRoute {
    internal val domain = "https://bbs.yamibo.com/"
    abstract fun build(): String

    fun toFullLink(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) path
        else "$domain${path.removePrefix("/")}"
    }

    data object Domain : YamiboRoute() {
        override fun build(): String = domain
    }

    data object Home : YamiboRoute() {
        override fun build(): String = domain
    }

    sealed class UserSpace {
        data class ProfileInfo(val userId: UserId? = null) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "space")
                        //For some unknown reason, the uid field can be empty.
                        parameters.append("uid", userId?.value?.toString() ?: "")
                        parameters.append("mycenter", "1")
                        parameters.append("mobile", "2")
                    }
                    .buildString()
            }
        }


        /**
         * Ta的主題
         */
        enum class ThreadType {
            /**
             * 主題
             */
            Thread,
            /**
             * 回復
             */
            Reply
        }

        data class Thread(val userId: UserId?,val type: ThreadType, val page: Int = 1) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "space")
                        parameters.append("uid", userId?.value?.toString() ?: "")
                        parameters.append("do", "thread")
                        parameters.append("view", "me")
                        parameters.append("order", "dateline")
                        if (type == ThreadType.Reply) parameters.append("type", "reply")
                        parameters.append("page", page.toString())
                        parameters.append("mobile", "2")
                    }.buildString()
            }
        }

        sealed class Blog {
            /**
             * 好友的日志
             *
             * It can only use on our own user page.
             *
             * DO NOT USE IT ON OTHER USER.
             */
            data class FriendBlog(val page: Int = 1) : YamiboRoute() {
                override fun build(): String {
                    return URLBuilder(domain)
                        .apply {
                            encodedPath = "home.php"
                            parameters.append("mod", "space")
                            parameters.append("do", "blog")
                            parameters.append("view", "we")
                            parameters.append("page", page.toString())
                            parameters.append("mobile", "2")
                        }.buildString()
                }
            }

            /**
             * 我的日志
             */
            data class MyBlog(val userId: UserId?, val page: Int = 1) : YamiboRoute() {
                override fun build(): String {
                    return URLBuilder(domain)
                        .apply {
                            encodedPath = "home.php"
                            parameters.append("mod", "space")
                            parameters.append("do", "blog")
                            parameters.append("view", "me")
                            if (userId != null) parameters.append("uid", userId.value.toString())
                            parameters.append("page", page.toString())
                            parameters.append("mobile", "2")
                        }.buildString()
                }
            }

            enum class ViewAllType {
                /**
                 * 最新發表的日治
                 */
                Latest,
                /**
                 * 推薦閱讀的日志
                 */
                Hot
            }

            /**
             * 隨便看看
             */
            data class ViewAll(val type: ViewAllType, val page: Int = 1) : YamiboRoute() {
                override fun build(): String {
                    return URLBuilder(domain).apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "space")
                        parameters.append("do", "blog")
                        parameters.append("view", "all")
                        if (type == ViewAllType.Hot) parameters.append("order", "hot")
                        parameters.append("page", page.toString())
                        parameters.append("mobile", "2")
                    }.buildString()
                }
            }
        }

        enum class NotificationType {
            /**
             * 我的消息
             */
            MyMessage,
            /**
             * 我的提醒
             */
            MyNotice
        }

        data class Notification(val type: NotificationType, val page: Int) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain).apply {
                    encodedPath = "home.php"
                    parameters.append("mod", "space")
                    when(type) {
                        NotificationType.MyMessage -> parameters.append("do", "pm")
                        NotificationType.MyNotice -> parameters.append("do", "notice")
                    }
                    parameters.append("page", page.toString())
                    parameters.append("mobile", "2")
                }.buildString()
            }
        }

        enum class FriendPageType {
            /**
             * 我的好友
             */
            MyFriend,
            /**
             * 在線成員
             */
            OnlineMember,

            /**
             * 我的訪客
             */
            MyVisitor,
            /**
             * 我的足跡
             */
            MyTrace,
        }

        data class MyFriend(val type: FriendPageType, val page: Int = 1): YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain).apply {
                    encodedPath = "home.php"
                    parameters.append("mod", "space")
                    parameters.append("do", "friend")
                    when(type) {
                        FriendPageType.OnlineMember -> {
                            parameters.append("view", "online")
                            parameters.append("type", "member")
                        }
                        FriendPageType.MyVisitor -> parameters.append("view", "visitor")
                        FriendPageType.MyTrace -> parameters.append("view", "trace")
                        else -> {}
                    }
                    parameters.append("page", page.toString())
                    parameters.append("mobile", "2")
                }.buildString()
            }
        }
    }

    data object MaintainingImage : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain).apply { encodedPath = "images/backup01.jpg" }.buildString()
        }
    }

    data class Forum(val fid: ForumId, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "forumdisplay")
                    parameters.append("fid", fid.value.toString())
                    parameters.append("page", page.toString())
                    parameters.append("mobile", "2")
                }
                .buildString()
        }
    }

    data class Thread(val tid: ThreadId, val authorId: UserId? = null, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "viewthread")
                    parameters.append("tid", tid.value.toString())
                    parameters.append("extra", "page=2")
                    parameters.append("page", page.toString())
                    parameters.append("authorid", authorId?.value?.toString() ?: "")
                    parameters.append("mobile", "2")
                }
                .buildString()
        }
    }

    /**
     * This is a Desktop Page.
     */
    data class TagPage(val tagId: TagId, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "misc.php"
                    parameters.append("mod", "tag")
                    parameters.append("id", tagId.value.toString())
                    parameters.append("type", "thread")
                    parameters.append("page", page.toString())
                }.buildString()
        }
    }

    sealed class Search : YamiboRoute() {
        data class SearchPhp(val forumId: ForumId?) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "search.php"
                        if (forumId == null) {
                            parameters.append("mod", "forum")
                        } else {
                            parameters.append("mod", "curforum")
                            parameters.append("srhfid", forumId.value.toString())
                        }
                    }
                    .buildString()
            }
        }

        data class BySearchId(val query: String, val searchId: SearchId, val page: Int = 1) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "search.php"
                        parameters.append("mod", "forum")
                        parameters.append("searchid", searchId.value.toString())
                        parameters.append("orderby", "dateline")
                        parameters.append("ascdesc", "desc")
                        parameters.append("searchsubmit", "yes")
                        parameters.append("kw", query)
                        parameters.append("mobile", "2")
                        parameters.append("page", page.toString())
                    }
                    .buildString()
            }
        }

        /**
         * @param location(e.g.
         * search.php?mod=forum&searchid=38813&orderby=dateline&ascdesc=desc&searchsubmit=yes&kw=%E9%AA%A8%E7%A7%91&mobile=2)
         * this param is get from "search.php?mod=forum" POST request's Location header.
         */
        data class ByLocation(val location: String, val page: Int = 1) : YamiboRoute() {
            override fun build(): String = "$domain$location&page=$page"
        }
    }

    sealed class Favorite : YamiboRoute() {
        data class GetFolder(val userId: UserId?, val type: FavoriteType, val page: Int = 1) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "space")
                        parameters.append("uid", userId?.value?.toString() ?: "")
                        parameters.append("do", "favorite")
                        parameters.append("view", "me")
                        parameters.append("type", type.typeId)
                        parameters.append("page", page.toString())
                        parameters.append("mobile", "2")
                    }.buildString()
            }
        }

        /**
         * This is a POST request.
         *
         * Content Type : application/x-www-form-urlencoded; charset=UTF-8
         *
         * FormData
         *
         * favoritesubmit=true&referer=(thread url)&formhash=(user formHash)&description=(手机收藏)
         */
        data class AddThread(val threadId: ThreadId) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "spacecp")
                        parameters.append("ac", "favorite")
                        parameters.append("type", FavoriteType.Thread.typeId)
                        parameters.append("id", threadId.value.toString())
                        parameters.append("spaceuid", "0")
                        parameters.append("mobile", "2")
                        parameters.append("handlekey", "favoriteform_${threadId.value}")
                        parameters.append("inajax", "1")
                    }.buildString()
            }
        }

        data class AddForum(val forumId: ForumId, val formHash: FormHash) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "spacecp")
                        parameters.append("ac", "favorite")
                        parameters.append("type", FavoriteType.Forum.typeId)
                        parameters.append("id", forumId.value.toString())
                        parameters.append("handlekey", "favoriteforum")
                        parameters.append("formhash", formHash.value)
                        parameters.append("mobile", "2")
                    }.buildString()
            }
        }

        data class Delete(val favoriteId: FavoriteId) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "spacecp")
                        parameters.append("ac", "favorite")
                        parameters.append("op", "delete")
                        parameters.append("favid", favoriteId.value.toString())
                        parameters.append("type", "all")
                        parameters.append("mobile", "2")
                        parameters.append("handlekey", "favoriteform_${favoriteId.value}")
                        parameters.append("inajax", "1")
                    }.buildString()
            }
        }
    }

    data class FindPost(val authorId: UserId?, val threadId: ThreadId?, val postId: PostId): YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "redirect")
                    parameters.append("goto", "findpost")
                    parameters.append("ptid", threadId?.value?.toString() ?: "")
                    parameters.append("pid", postId.value.toString())
                    parameters.append("fromuid", authorId?.value?.toString() ?: "")
                }.buildString()
        }
    }

    data class UserInfoPage(val userId: UserId) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "home.php"
                    parameters.append("mod", "space")
                    parameters.append("uid", userId.value.toString())
                    parameters.append("do", "profile")
                    parameters.append("mobile", "2")
                }
                .buildString()
        }
    }

    /**
     * Blog detail page.
     */
    data class BlogPage(val blogId: BlogId, val userId: UserId? = null, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "home.php"
                    parameters.append("mod", "space")
                    parameters.append("uid", userId?.value?.toString() ?: "")
                    parameters.append("do", "blog")
                    parameters.append("id", blogId.value.toString())
                    parameters.append("page", page.toString())
                    parameters.append("mobile", "2")
                }
                .buildString()
        }
    }

    /**
     * Comment on a blog.
     *
     * This is a POST request.
     */
    data object BlogComment : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "home.php"
                    parameters.append("mod", "spacecp")
                    parameters.append("ac", "comment")
                }
                .buildString()
        }
    }

    /**
     * 評分帖子.
     *
     * This is a POST request.
     *
     * Content-Type : application/x-www-form-urlencoded; charset=UTF-8
     *
     * FormData
     *
     * formhash=(formHash)&tid=(thread id)&pid=(post id)&referer=(thread link)&handlekey=rate&score1=+(score)&reason=(reason)
     */
    data object Rate : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "misc")
                    parameters.append("action", "rate")
                    parameters.append("ratesubmit", "yes")
                    parameters.append("infloat", "yes")
                    parameters.append("inajax", "1")
                    parameters.append("handlekey", "rateform")
                    parameters.append("inajax", "1")
                }.buildString()
        }
    }

    /**
     * Rating popout form.
     *
     * This is a GET request.
     */
    data class RatePopout(val threadId: ThreadId, val postId: PostId) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "misc")
                    parameters.append("action", "rate")
                    parameters.append("tid", threadId.value.toString())
                    parameters.append("pid", postId.value.toString())
                    parameters.append("mobile", "2")
                    parameters.append("infloat", "yes")
                    parameters.append("handlekey", "rate")
                    parameters.append("inajax", "1")
                }.buildString()
        }
    }

    /**
     * 點評帖子.
     *
     * This is a POST request.
     * @param page It seems like the number is actually doesnt matter idk.
     *
     * Content-Type : application/x-www-form-urlencoded; charset=UTF-8
     *
     * FormData
     *
     * formhash=(formHash)&handlekey=&message=(message)
     */
    data class PostComment(val threadId: ThreadId, val postId: PostId, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "post")
                    parameters.append("action", "reply")
                    parameters.append("comment", "yes")
                    parameters.append("tid", threadId.value.toString())
                    parameters.append("pid", postId.value.toString())
                    parameters.append("extra", "")
                    parameters.append("page", page.toString())
                    parameters.append("commentsubmit", "yes")
                    parameters.append("infloat", "yes")
                    parameters.append("inajax", "1")
                    parameters.append("handlekey", "commentform")
                    parameters.append("inajax", "1")
                }.buildString()
        }
    }

    /**
     * 針對某帖子回復
     *
     * This is only for building a url for webview now.
     */
    data class PostReply(val threadId: ThreadId, val postId: PostId, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "post")
                    parameters.append("action", "reply")
                    parameters.append("tid", threadId.value.toString())
                    parameters.append("repquote", postId.value.toString())
                    parameters.append("extra", "")
                    parameters.append("page", page.toString())
                    parameters.append("mobile", "2")
                }.buildString()
        }
    }

    /**
     * 回復整個討論串(不引用)
     *
     * This is only for building a url for webview now.
     */
    data class ThreadReply(val threadId: ThreadId, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "post")
                    parameters.append("action", "reply")
                    parameters.append("tid", threadId.value.toString())
                    parameters.append("reppost", "0")
                    parameters.append("page", page.toString())
                    parameters.append("mobile", "2")
                }.buildString()
        }
    }

    /**
     * 投票帖子.
     *
     * This is a POST request.
     *
     * Content-Type : application/x-www-form-urlencoded; charset=UTF-8
     *
     * FormData
     * formhash=(formHash)&pollanswers[]=(option id)
     */
    data class VotePoll(val forumId: ForumId, val threadId: ThreadId) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "misc")
                    parameters.append("action", "votepoll")
                    parameters.append("fid", forumId.value.toString())
                    parameters.append("tid", threadId.value.toString())
                    parameters.append("pollsubmit", "yes")
                    parameters.append("quickforward", "yes")
                    parameters.append("mobile", "2")
                    parameters.append("handlekey", "poll")
                    parameters.append("inajax", "1")
                }.buildString()
        }
    }

    /**
     * Post新的Thread.
     *
     * This is only for building a url for webview now.
     * https://bbs.yamibo.com/forum.php?mod=post&action=newthread&fid=55&mobile=2
     */
    data class PostThread(val forumId: ForumId) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "post")
                    parameters.append("action", "newthread")
                    parameters.append("fid", forumId.value.toString())
                    parameters.append("mobile", "2")
                }.buildString()
        }
    }

    /**
     * 發私信
     *
     * Only for WebView.
     */
    data object SendPrivateMessagePage : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "home.php"
                    parameters.append("mod", "spacecp")
                    parameters.append("ac", "pm")
                    parameters.append("mobile", "2")
                }.buildString()
        }
    }

    /**
     * 發送日誌
     *
     * Only for WebView.
     */
    data object SendBlogPage : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "home.php"
                    parameters.append("mod", "spacecp")
                    parameters.append("ac", "blog")
                    parameters.append("mobile", "2")
                }.buildString()
        }
    }

    data object Login : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "member.php"
                    parameters.append("mod", "logging")
                    parameters.append("action", "login")
                    parameters.append("mobile", "2")
                }.buildString()
        }
    }


    data object Sign : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "plugin.php"
                    parameters.append("id", "zqlj_sign")
                }.buildString()
        }
    }
}
