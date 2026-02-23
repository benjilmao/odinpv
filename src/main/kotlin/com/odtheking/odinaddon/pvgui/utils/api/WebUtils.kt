package com.odtheking.odinaddon.pvgui.utils.api

import com.google.gson.JsonParser
import com.odtheking.odin.OdinMod.logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.coroutines.resume

// Based on code from OdinFabric by odtheking
// https://github.com/odtheking/OdinFabric
// Licensed under BSD-3-Clause
// Modified to use kotlinx.serialization for proper Kotlin default handling

object WebUtils {
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend inline fun <reified T : Any> fetchJson(url: String): Result<T> = runCatching {
        val response = fetchString(url).getOrElse { return Result.failure(it) }
        json.decodeFromString<T>(response)
    }

    suspend fun fetchString(url: String): Result<String> =
        executeRequest(createGetRequest(url))
            .mapCatching { it.body() }
            .onFailure { logger.warn("Failed to fetch from $url: ${it.message}") }

    private fun createGetRequest(url: String): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build()

    private suspend fun executeRequest(request: HttpRequest): Result<HttpResponse<String>> =
        suspendCancellableCoroutine { cont ->
            logger.info("Making request to ${request.uri()}")
            val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            cont.invokeOnCancellation {
                logger.info("Cancelling request to ${request.uri()}")
                future.cancel(true)
            }
            future.whenComplete { response, error ->
                when {
                    error != null -> {
                        if (cont.isActive) {
                            logger.warn("Request failed for ${request.uri()}: ${error.message}")
                            cont.resume(Result.failure(error))
                        }
                    }
                    response.statusCode() in 200..299 -> {
                        if (cont.isActive) cont.resume(Result.success(response))
                    }
                    else -> {
                        if (cont.isActive) cont.resume(Result.failure(
                            InputStreamException(response.statusCode(), request.uri().toString())
                        ))
                    }
                }
            }
        }

    class InputStreamException(code: Int, url: String) : Exception("Failed to get input stream from $url: HTTP $code")
}