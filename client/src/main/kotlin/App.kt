package com.perpheads.files

import com.perpheads.files.components.*
import kotlinext.js.jso
import kotlinx.browser.document
import kotlinx.browser.window
import react.Props
import react.createElement
import react.dom.render
import react.router.NavigateFunction
import react.router.Route
import react.router.Routes
import react.router.dom.*

external interface AccountProps : Props {
    var page: Int
}

fun NavigateFunction.replace(route: String) {
    this(route, jso { replace = true })
}

fun logout(navigate: NavigateFunction) {
    window.localStorage.removeItem("loggedIn")
    navigate.replace("/")
}

inline fun logoutIfUnauthorized(navigate: NavigateFunction, block: () -> Unit) {
    try {
        block()
    } catch (e: ApiClient.UnauthorizedException) {
        logout(navigate)
    }
}

fun main() {
    window.onload = {
        document.getElementById("root")?.let { rootElem ->
            render(rootElem) {
                HashRouter {
                    Routes {
                        Route {
                            attrs.path = "/account"
                            attrs.element = createElement(AccountPageComponent)
                        }
                        Route {
                            attrs.path = "/change-password"
                            attrs.element = createElement(ChangePasswordComponent)
                        }
                        Route {
                            attrs.path = "/api-key"
                            attrs.element = createElement(ApiKeyComponent)
                        }
                        Route {
                            attrs.path = "/share"
                            attrs.element = createElement(ShareComponent)
                        }
                        Route {
                            attrs.path = "/share/:token"
                            attrs.element = createElement(ShareDownloadComponent)
                        }
                        Route {
                            attrs.path = "/"
                            attrs.element = createElement(LoginPageComponent)
                        }
                    }
                }
            }
        }
    }
}