package io.github.littlesurvival

import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.ktor.http.*
import kotlin.toString

sealed class YamiboRoute {
    internal val domain = "https://bbs.yamibo.com/"
    abstract fun build(): String

    data object Domain : YamiboRoute() {
        override fun build(): String = domain
    }

    data object Home : YamiboRoute() {
        override fun build(): String = domain
    }

    data object ProfileInfo : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "spacecp")
                        parameters.append("ac", "credit")
                        parameters.append("showcredit", "1")
                    }
                    .buildString()
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

    data class Thread(val tid: ThreadId, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                    .apply {
                        encodedPath = "forum.php"
                        parameters.append("mod", "viewthread")
                        parameters.append("tid", tid.value.toString())
                        parameters.append("extra", "page=2")
                        parameters.append("page", page.toString())
                        parameters.append("mobile", "2")
                    }
                    .buildString()
        }
    }

    sealed class Search : YamiboRoute() {
        data object SearchPhp : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                        .apply {
                            encodedPath = "search.php"
                            parameters.append("mod", "forum")
                        }
                        .buildString()
            }
        }
        data class ById(val query: String, val searchId: SearchId) : YamiboRoute() {
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
                        }
                        .buildString()
            }
        }

        /**
         * @param location(e.g.
         * search.php?mod=forum&searchid=38813&orderby=dateline&ascdesc=desc&searchsubmit=yes&kw=%E9%AA%A8%E7%A7%91&mobile=2)
         * this param is get from "search.php?mod=forum" POST request's Location header.
         */
        data class ByLocation(val location: String) : YamiboRoute() {
            override fun build(): String = domain + location
        }
    }

    sealed class Favorite : YamiboRoute() {
        data class GetFolder(val userId: UserId, val type: FavoriteType,val page: Int = 1) : YamiboRoute() {
            override fun build(): String {
                return URLBuilder(domain)
                    .apply {
                        encodedPath = "home.php"
                        parameters.append("mod", "space")
                        parameters.append("uid", userId.value.toString())
                        parameters.append("do", "favorite")
                        parameters.append("view", "me")
                        parameters.append("type", type.typeId)
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
     * 點評帖子.
     *
     * This is a POST request.
     * @param page It's seems like the number is actually doesnt matter idk.
     *
     * Content-Type : application/x-www-form-urlencoded; charset=UTF-8
     *
     * FormData
     *
     * formhash=(formHash)&handlekey=&message=(message)
     */
    data class PostReply(val tId: ThreadId, val pId: PostId, val page: Int = 1) : YamiboRoute() {
        override fun build(): String {
            return URLBuilder(domain)
                .apply {
                    encodedPath = "forum.php"
                    parameters.append("mod", "post")
                    parameters.append("action", "reply")
                    parameters.append("comment", "yes")
                    parameters.append("tid", tId.value.toString())
                    parameters.append("pid", pId.value.toString())
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
