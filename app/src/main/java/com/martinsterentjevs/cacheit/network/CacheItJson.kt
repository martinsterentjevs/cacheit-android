package com.martinsterentjevs.cacheit.network

import kotlinx.serialization.json.Json

    val cacheItJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
