package com.github.api

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseAsset(
    val url: String,
    val browser_download_url: String,
    val id: Long,
    val node_id: String,
    val name: String,
    val label: String? = null,
    val state: String,
    val content_type: String,
    val size: Long,
    val download_count: Long,
    val created_at: String,
    val uploaded_at: String? = null,
    val uploader: User
)
