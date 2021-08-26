package com.perpheads.files

import com.perpheads.files.components.accountPage
import com.perpheads.files.components.apiKeyPage
import com.perpheads.files.components.changePasswordPage
import com.perpheads.files.components.loginPageComponent
import kotlinx.browser.document
import kotlinx.browser.window
import react.Props
import react.dom.render
import react.router.dom.*

external interface AccountProps : Props {
    var page: Int
}

fun logout(history: History) {
    window.localStorage.removeItem("loggedIn")
    history.replace("/")
}

inline fun logoutIfUnauthorized(history: History, block: () -> Unit) {
    try {
        block()
    } catch (e: ApiClient.UnauthorizedException) {
        logout(history)
    }
}

fun main() {
    window.onload = {
        render(document.getElementById("root")) {
            hashRouter {
                switch {
                    route("/account") {
                        accountPage {}
                    }
                    route("/change-password") {
                        changePasswordPage()
                    }
                    route("/api-key") {
                        apiKeyPage()
                    }
                    route("/", exact = true) {
                        if (window.localStorage.getItem("loggedIn") == "yes") {
                            redirect(to = "/account")
                        } else {
                            loginPageComponent()
                        }
                    }
                }
            }
        }
    }
}