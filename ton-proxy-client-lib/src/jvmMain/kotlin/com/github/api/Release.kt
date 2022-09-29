package com.github.api

import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val url: String,
    val html_url: String,
    val assets_url: String,
    val upload_url: String,
    val tarball_url: String? = null,
    val zipball_url: String? = null,
    val id: Long,
    val node_id: String,
    val tag_name: String,
    val target_commitish: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean,
    val prerelease: Boolean,
    val created_at: String,
    val published_at: String? = null,
    val author: User,
    val assets: List<ReleaseAsset> = emptyList(),
)
