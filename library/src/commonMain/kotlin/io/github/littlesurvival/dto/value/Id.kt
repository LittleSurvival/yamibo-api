package io.github.littlesurvival.dto.value

import kotlin.jvm.JvmInline

/** Type-safe forum id (fid). */
@JvmInline value class ForumId(val value: Int)

/** Type-safe thread id (tid). */
@JvmInline value class ThreadId(val value: Int)

/** Type-safe post id (pid). */
@JvmInline value class PostId(val value: Int)

/** Type-safe user id (uid). */
@JvmInline value class UserId(val value: Int)

/** Type-safe search id(sid) */
@JvmInline value class SearchId(val value: Int)