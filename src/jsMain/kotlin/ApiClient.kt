package com.perpheads.files

import com.perpheads.files.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.MainScope

object ApiClient {
    val mainScope = MainScope()

    object UnauthorizedException : Exception()

    private val client = HttpClient(Js) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }

        HttpResponseValidator {
            handleResponseException { exception ->
                if (exception !is ClientRequestException) return@handleResponseException
                val exceptionResponse = exception.response
                if (exceptionResponse.status == HttpStatusCode.Unauthorized) {
                    throw UnauthorizedException
                }
            }
        }
    }

    suspend fun loadFiles(
        query: String = "",
        beforeId: Int? = null,
        page: Int? = null,
        entriesPerPage: Int = 9
    ): FileListResponse {
        return client.post(window.location.origin + "/account") {
            parameter("include_thumbnails", false)
            contentType(ContentType.Application.Json)
            body = SearchRequest(
                query = query,
                beforeId = beforeId,
                page = page,
                entriesPerPage = entriesPerPage
            )
        }
    }

    suspend fun getLoggedIn(): Boolean {
        return try {
            getAccountInfo()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFile(link: String) {
        return client.delete(window.location.origin + "/${link}")
    }

    suspend fun getAccountInfo(): AccountInfo {
        return client.get(window.location.origin + "/account-info")
    }

    suspend fun getApiKey(): ApiKeyResponse {
        return client.get(window.location.origin + "/api-key")
    }

    suspend fun generateApiKey(): ApiKeyResponse {
        return client.post(window.location.origin + "/generate-api-key")
    }

    suspend fun authenticate(username: String, password: String, remember: Boolean): LoginResponse {
        return client.post(window.location.origin + "/auth") {
            contentType(ContentType.Application.Json)
            body = LoginRequest(username, password, remember)
        }
    }

    suspend fun logout() = client.post<Unit>(window.location.origin + "/logout")

    suspend fun changePassword(existingPassword: String, newPassword: String) {
        return client.post(window.location.origin + "/change-password") {
            contentType(ContentType.Application.Json)
            body = ChangePasswordRequest(existingPassword, newPassword)
        }
    }
}