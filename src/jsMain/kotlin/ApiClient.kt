package com.perpheads.files

import com.perpheads.files.data.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import org.w3c.fetch.INCLUDE
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.files.File
import org.w3c.xhr.FormData
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

    suspend fun uploadFile(file: File, onProgress: (Double) -> Unit): FileResponse {
        return suspendCoroutine { continuation ->
            val formData = FormData()
            formData.append("file", file, file.name)
            val xmlRequest = XMLHttpRequest()
            xmlRequest.onprogress = {
                val progress = it.loaded.toDouble() / file.size.toDouble()
                onProgress(progress)
            }
            xmlRequest.onerror = {
                continuation.resumeWithException(RuntimeException("Unknown error while fetching data"))
            }
            xmlRequest.onreadystatechange = {
                console.log("Ready state changed: ${xmlRequest.readyState}")
                if (xmlRequest.readyState == 4.toShort()) {
                    when (xmlRequest.status) {
                        200.toShort() -> {
                            try {
                                val jsonResponse = xmlRequest.responseText
                                continuation.resume(Json.decodeFromString(jsonResponse))
                            } catch (e: Exception) {
                                continuation.resumeWithException(RuntimeException("Unknown error when fetching data"))
                            }
                        }
                        401.toShort() -> continuation.resumeWithException(UnauthorizedException)
                        else -> continuation.resumeWithException(RuntimeException("Unknown error when fetching data"))
                    }
                    xmlRequest.response
                }
            }
            xmlRequest.open("POST", "/upload-cookie")
            xmlRequest.send(formData)
        }
    }
}