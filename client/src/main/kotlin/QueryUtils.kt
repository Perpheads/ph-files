package com.perpheads.files

import kotlinx.browser.window

external fun decodeURIComponent(str: String): String
external fun encodeURIComponent(str: String): String

fun parseQueryString(str: String): Map<String, String> {
    val pairs = str.split("&")
    return pairs.mapNotNull {
        val pair = it.split("=")
        if (pair.size != 2) return@mapNotNull null
        decodeURIComponent(pair[0]) to decodeURIComponent(pair[1])
    }.toMap()
}

class Parameters private constructor() {
    private val parameterMap = mutableMapOf<String, String>()

    fun set(key: String, value: String) {
        parameterMap[key] = value
    }

    private fun formURLEncode(): String {
        return parameterMap.map {
            encodeURIComponent(it.key) + "=" + encodeURIComponent(it.value)
        }.joinToString("&")
    }

    companion object {
        fun build(body: (Parameters).() -> Unit): String {
            val params = Parameters()
            params.body()
            return params.formURLEncode()
        }
    }
}