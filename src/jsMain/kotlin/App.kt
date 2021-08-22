package com.perpheads.files

import com.perpheads.files.components.*
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.Color
import kotlinx.css.color
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.button
import react.dom.render
import react.functionComponent
import react.router.dom.hashRouter
import react.router.dom.route
import react.router.dom.switch
import react.router.dom.useLocation
import react.useState
import styled.css
import styled.styledDiv

external interface AccountProps : RProps {
    var page: Int
}

fun main() {
    window.onload = {
        render(document.getElementById("root")) {
            hashRouter {
                switch {
                    /*route<AccountProps>("/account/:page") { props ->
                        accountPage {
                            page = props.match.params.page
                        }
                    }*/
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
                        loginPageComponent()
                    }
                }
            }
        }
    }
}