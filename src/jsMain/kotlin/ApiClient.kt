package com.perpheads.files

import com.perpheads.files.data.LoginRequest
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*

object ApiClient {
    private val client = HttpClient(Js) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    suspend fun authenticate(username: String, password: String, remember: Boolean): String {
        return client.post("/auth") {
            contentType(ContentType.Application.Json)
            body = LoginRequest(username, password, remember)
        }
    }
}