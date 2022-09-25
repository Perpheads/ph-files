package com.perpheads.files

import com.perpheads.files.data.*
import com.perpheads.files.wrappers.axios
import com.perpheads.files.wrappers.axiosDelete
import com.perpheads.files.wrappers.axiosGet
import com.perpheads.files.wrappers.axiosPost
import com.perpheads.files.data.ShareFileResponse
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.css.em
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.files.File
import org.w3c.xhr.FormData
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ApiClient {
    val mainScope = MainScope()

    object UnauthorizedException : Exception()
    object NotFoundException : Exception()
    data class UnexpectedHttpStatusException(val status: Int) : Exception()

    suspend fun loadFiles(
        query: String = "",
        beforeId: Int? = null,
        page: Int? = null,
        entriesPerPage: Int = 9
    ): FileListResponse {
        axios.create()
        val request = SearchRequest(
            query = query,
            beforeId = beforeId,
            page = page,
            entriesPerPage = entriesPerPage
        )
        val result = axiosPost<FileListResponse, SearchRequest>("/account", request) {
            parameter("include_thumbnails", "false")
        }
        return result
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
        return axiosDelete("/${link}")
    }

    suspend fun getAccountInfo(): AccountInfoV2 {
        return axiosGet("/account-info")
    }

    suspend fun getApiKey(): ApiKeyResponse {
        return axiosGet("/api-key")
    }

    suspend fun generateApiKey(): ApiKeyResponse {
        return axiosPost("/generate-api-key")
    }

    suspend fun getStatistics(): StatisticsResponse {
        return axiosGet("/statistics")
    }

    suspend fun authenticate(username: String, password: String, remember: Boolean): LoginResponseV2 {
        val body = LoginRequest(username, password, remember)
        return axiosPost("/v2/auth", body)
    }

    suspend fun createUser(username: String, email: String, password: String) {
        val body = CreateUserRequest(
            username = username,
            email = email,
            password = password
        )
        return axiosPost("/users", body)
    }

    suspend fun getContact(): ContactResponse {
        return axiosGet("/contact")
    }

    suspend fun logout() = axiosPost<Unit>("/logout")

    suspend fun changePassword(existingPassword: String, newPassword: String) {
        val body = ChangePasswordRequest(existingPassword, newPassword)
        return axiosPost("/change-password", body)
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

    suspend fun getSharedFileInformation(link: String): ShareFileResponse? {
        return try {
            axiosGet("/share/${link}")
        } catch (e: NotFoundException) {
            null
        }
    }

}