package com.example.livekitdemo.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TokenResponse(val token: String, val serverUrl: String)

object TokenApiClient {

    suspend fun fetchToken(baseUrl: String, room: String, identity: String): TokenResponse =
        withContext(Dispatchers.IO) {
            val query = "room=${URLEncoder.encode(room, "UTF-8")}&identity=${URLEncoder.encode(identity, "UTF-8")}"
            val url = URL("${baseUrl.trimEnd('/')}/token?$query")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }

                if (responseCode !in 200..299) {
                    val message = runCatching { JSONObject(body).getString("error") }.getOrDefault(body)
                    error("Token server error ($responseCode): $message")
                }

                val json = JSONObject(body)
                TokenResponse(
                    token = json.getString("token"),
                    serverUrl = json.getString("serverUrl"),
                )
            } finally {
                connection.disconnect()
            }
        }
}
