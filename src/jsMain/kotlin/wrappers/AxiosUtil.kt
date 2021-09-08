package com.perpheads.files.wrappers

import com.perpheads.files.ApiClient
import kotlinext.js.Object
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.json

inline fun <reified U> generateConfig(ct: CancelToken? = null) = object : AxiosRequestConfig {
    override var cancelToken = ct

    override var transformRequest: Array<(U, dynamic) -> String> = arrayOf(
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

    override var transformResponse: (String) -> String = { it }

    /*
    val response = if (U::class == Unit::class) {
        Unit as U
    } else if (it is U) {
        it
    } else {
        Json.decodeFromString<U>(it)
    }
    response*/
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

inline fun <reified U> setupAxiosConfig(configure: (AxiosConfigScope).() -> Unit): AxiosRequestConfig {
    val config = generateConfig<U>()
    val configScope = AxiosConfigScope(config)
    configScope.configure()
    configScope.apply()
    return config
}

suspend inline fun <reified T> AxiosPromise<String>.handle(): T {
    return try {
        val response = this.await()
        if (T::class == Unit::class) {
            Unit as T
        } else if (T::class == String::class) {
            response.data as T
        } else {
            Json.decodeFromString(response.data)
        }
    } catch (e: dynamic) {
        when (val statusCode = e.response.status.unsafeCast<Int>()) {
            401 -> throw ApiClient.UnauthorizedException
            404 -> throw ApiClient.NotFoundException
            else -> throw ApiClient.UnexpectedHttpStatusException(statusCode)
        }
    }
}

suspend inline fun <reified T> axiosGet(url: String, configure: (AxiosConfigScope).() -> Unit = {}): T {
    val config = setupAxiosConfig<Unit>(configure)
    return axios.get<String>(url, config).handle()
}

suspend inline fun <reified T, reified U : Any> axiosPost(
    url: String,
    body: U,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<U>(configure)
    return axios.post<String>(url, body, config).handle()
}

suspend inline fun <reified T> axiosPost(
    url: String,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<Unit>(configure)
    return axios.post<String>(url, Unit, config).handle()
}

suspend inline fun <reified T, reified U : Any> axiosPut(
    url: String,
    body: U,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<U>(configure)
    return axios.put<String>(url, body, config).handle()
}

suspend inline fun <reified T> axiosDelete(
    url: String,
    configure: (AxiosConfigScope).() -> Unit = {}
): T {
    val config = setupAxiosConfig<Unit>(configure)
    return axios.delete<String>(url, config).handle()
}
