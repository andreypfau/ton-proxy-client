package com.github.api

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val login: String,
    val id: Long
)
