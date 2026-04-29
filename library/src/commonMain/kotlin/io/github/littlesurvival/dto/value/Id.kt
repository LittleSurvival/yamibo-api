package io.github.littlesurvival.dto.value

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

interface Id
/** Type-safe forum id (fid). */
@Serializable
@JvmInline value class ForumId(val value: Int) : Id

/** Type-safe thread id (tid). */
@Serializable
@JvmInline value class ThreadId(val value: Int) : Id

/** Type-safe post id (pid). */
@Serializable
@JvmInline value class PostId(val value: Int) : Id

/** Type-safe user id (uid). */
@Serializable
@JvmInline value class UserId(val value: Int) : Id

/** Type-safe search id(sid) */
@Serializable
@JvmInline value class SearchId(val value: Int) : Id

/**
 * Type-safe favorite id (fvid)
 */
@Serializable
@JvmInline value class FavoriteId(val value: Int) : Id

/**
 * Type-safe tag id
 */
@Serializable
@JvmInline value class TagId(val value: Int) : Id

/**
 * Type-safe blog id
 */
@Serializable
@JvmInline value class BlogId(val value: Int) : Id

/**
 * Type-safe notice id
 */
@Serializable
@JvmInline value class NoticeId(val value: Int) : Id
