package com.perpheads.files.wrappers

import com.perpheads.files.ApiClient
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.json

inline fun <reified T, reified U> generateConfig(ct: CancelToken? = null) = object : AxiosRequestConfig {
    override var cancelToken = ct
    override var transformRequest: Array<(T, dynamic) -> String> = arrayOf(
        { data, headers ->
            when {
                data === undefined -> ""
                data is Unit -> ""
                data is String -> data
                else -> {
                    headers["Content-Type"] = "application/json"
                    Json.encodeToString(data)
                }
            }
        }
    )
    override var transformResponse: (String) -> U = {
        val response = if (U::class == Unit::class) {
            Unit as U
        } else if (it is U) {
            it
        } else {
            Json.decodeFromString<U>(it)
        }
        response
    }
}

class AxiosConfigScope(private val config: AxiosRequestConfig) {
    private val parameters = mutableMapOf<String, String>()
    private val headers = mutableMapOf<String, String>()

    fun header(key: String, value: String) {
        headers[key] = value
    }

    fun parameter(key: String, value: Any) {
        parameters[key] = value.toString()
    }

    fun apply() {
        config.headers = json(*headers.toList().toTypedArray())
        config.params = json(*parameters.toList().toTypedArray())
    }
}

inline fun <reified T, reified U : Any> setupAxiosConfig(configure: (AxiosConfigScope).() -> Unit): AxiosRequestConfig {
    val config = generateConfig<U, T>()
    val configScope = AxiosConfigScope(config)
    configScope.configure()
    configScope.apply()
    return config
}

fun <T> AxiosResponse<T>.handle(): T {
    return when (this.status.toInt()) {
        200 -> this.data
        401 -> throw ApiClient.UnauthorizedException
        404 -> throw ApiClient.NotFoundException
        else -> throw ApiClient.UnexpectedHttpStatusException(this.status.toInt())
    }
}

suspend inline fun <reified T> axiosGet(url: String, configure: (AxiosConfigScope).() -> Unit = {}): T {
    val config = setupAxiosConfig<T, Unit>(configure)
    return axios.get<T>(url, config).await().data
}

suspend inline fun <reified T, reified U : Any> axiosPost(
    url: String,
    body: U,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<T, U>(configure)
    return axios.post<T>(url, body, config).await().handle()
}

suspend inline fun <reified T> axiosPost(
    url: String,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<T, Unit>(configure)
    return axios.post<T>(url, Unit, config).await().handle()
}

suspend inline fun <reified T, reified U : Any> axiosPut(
    url: String,
    body: U,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<T, U>(configure)
    return axios.put<T>(url, body, config).await().handle()
}

suspend inline fun <reified T> axiosDelete(
    url: String,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<T, Unit>(configure)
    return axios.delete<T>(url, config).await().handle()
}
