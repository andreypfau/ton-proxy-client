package com.github.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.ton.proxy.utils.DefaultHttpClient

class GitHubApi(
    private val httpClient: HttpClient = DefaultHttpClient
) {
    companion object {
        const val API_URL = "https://api.github.com"
    }

    suspend fun getLatestRelease(repo: String): Release =
        httpClient.get("$API_URL/repos/$repo/releases/latest").body()
}
