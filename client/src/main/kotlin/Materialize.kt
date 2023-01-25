package com.perpheads.files

import js.core.jso


external interface ToastConfig {
    var html: String
}

external interface Materialize {
    fun toast(config: ToastConfig)
}

external val M: Materialize

fun showToast(text: String) {
    M.toast(jso {
        html = text
    })
}