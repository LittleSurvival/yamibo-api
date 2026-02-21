package io.github.littlesurvival

import io.github.littlesurvival.dto.value.ForumId

sealed class YamiboConstant {
    abstract fun build(): String
    /**
     * 論壇模板
     */
    sealed class Forum : YamiboConstant() {

        /**
         * 動漫區(id: 5)
         * 请不要在莉莉安女子学院里狂奔……你给我站住！！
         */
        sealed class AnimeArea(val page: Int = 1) : Forum() {
            override fun build(): String = YamiboRoute.Forum(ForumId(5), page).build()

            /**
             * 百合会最萌世界杯专版！(id: 52)
             */
            data class AdorableWorldCupArea(val page: Int = 1) : Forum() {
                override fun build(): String = YamiboRoute.Forum(ForumId(52), page).build()
            }
        }

        /**
         * 海域區(id: 33)
         * 风声水起。
         */
        data class SeaArea(val page: Int = 1) : Forum() {
            override fun build(): String = YamiboRoute.Forum(ForumId(33), page).build()
        }

        /**
         * 貼圖區(id: 13)
         * 玩悦图色。
         */
        sealed class StickerArea(val page: Int = 1) : Forum() {
            override fun build(): String = YamiboRoute.Forum(ForumId(13), page).build()

            /**
             * 原创图作区(id: 46)
             */
            data class OriginalWorkArea(val page: Int = 1) : Forum() {
                override fun build(): String = YamiboRoute.Forum(ForumId(46), page).build()
            }

            /**
             * 中文百合漫画区(id: 30)
             */
            data class TranslatedYuriMangaArea(val page: Int = 1) : Forum() {
                override fun build(): String = YamiboRoute.Forum(ForumId(30), page).build()
            }

            /**
             * 百合漫画图源区(id: 37)
             */
            data class YuriMangaSourceArea(val page: Int = 1) : Forum() {
                override fun build(): String = YamiboRoute.Forum(ForumId(37), page).build()
            }
        }

        /**
         * 文學區(id: 49)
         * 天方夜谭
         */
        sealed class LiteratureArea(val page: Int = 1) : Forum() {
            override fun build(): String = YamiboRoute.Forum(ForumId(49), page).build()

            /**
             * 轻小说/译文区(id: 55)
             */
            data class TranslatedLightNovalArea(val page: Int = 1) : Forum() {
                override fun build(): String = YamiboRoute.Forum(ForumId(55), page).build()
            }

            /**
             * TXT小说区(id: 60)
             */
            data class TxtNovelArea(val page: Int = 1) : Forum() {
                override fun build(): String = YamiboRoute.Forum(ForumId(60), page).build()
            }
        }

        /**
         * 遊戲區(id: 44)
         * 游戏人间
         */
        data class GamingArea(val page: Int = 1) : Forum() {
            override fun build(): String = YamiboRoute.Forum(ForumId(44), page).build()
        }

        /**
         * 影視區(id: 379)
         * 观剧磕糖
         */
        data class MovieVisualArea(val page: Int = 1) : Forum() {
            override fun build(): String = YamiboRoute.Forum(ForumId(379), page).build()
        }

        /**
         * 資源交流區(id: 19)
         * 海纳百川。
         */
        sealed class ResourceArea(val page: Int = 1) : Forum() {
            override fun build(): String = YamiboRoute.Forum(ForumId(19), page).build()

            /**
             * 非百合資源區(id: 27)
             */
            data class NonYuriResourceArea(val page: Int = 1) : Forum() {
                override fun build(): String = YamiboRoute.Forum(ForumId(27), page).build()
            }
        }
    }

    sealed class Thread : YamiboConstant() {

    }
}