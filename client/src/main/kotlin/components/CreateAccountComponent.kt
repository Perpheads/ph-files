package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.replace
import com.perpheads.files.showToast
import com.perpheads.files.useAccount
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.input
import kotlinx.css.margin
import kotlinx.css.px
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.Props
import react.dom.*
import react.fc
import react.router.useNavigate
import react.useEffect
import react.useState
import styled.css
import styled.styledDiv

val CreateAccountComponent = fc<Props>("CreateAccountComponent") {
    val (user, _) = useAccount()
    val navigate = useNavigate()
    var username by useState("")
    var email by useState("")
    var password by useState("")
    var buttonEnabled by useState(true)

    useEffect(user) {
        if (user != null && !user.admin) {
            navigate.replace("/")
        }
    }


    div {
        navBar {
            message = "Create a new Account"
            showSearchBar = false
            onSearchChanged = {}
        }

        if (user == null) return@div
        div("container") {
            styledDiv {
                css {
                    classes += "card fadeIn animated"
                }

                div("row") {
                    form(classes = "col s12") {
                        attrs.onSubmit = {
                            buttonEnabled = false
                            MainScope().launch {
                                try {
                                    ApiClient.createUser(username, email, password)
                                    showToast("New account created successfully")
                                    username = ""
                                    email = ""
                                    password = ""
                                } catch (e: Exception) {
                                    showToast("Failed creating new account")
                                }
                                buttonEnabled = true
                            }
                        }

                        div("row") {
                            div("input-field col s12") {
                                input(type = InputType.text, classes = "validate") {
                                    attrs.id = "username"
                                    attrs.value = username
                                    attrs.onChangeFunction = { username = (it.target as HTMLInputElement).value }
                                    attrs.minLength = "3"
                                    attrs.maxLength = "50"
                                }
                                label {
                                    attrs.htmlFor = "username"
                                    +"Username"
                                }
                            }
                        }
                        div("row") {
                            div("input-field col s12") {
                                input(type = InputType.email, classes = "validate") {
                                    attrs.id = "email"
                                    attrs.value = email
                                    attrs.onChangeFunction = { email = (it.target as HTMLInputElement).value }
                                    attrs.minLength = "3"
                                    attrs.maxLength = "100"
                                }
                                label {
                                    attrs.htmlFor = "email"
                                    +"Email"
                                }
                            }
                        }
                        div("row") {
                            div("input-field col s12") {
                                input(type = InputType.password, classes = "validate") {
                                    attrs.id = "password"
                                    attrs.value = password
                                    attrs.onChangeFunction = { password = (it.target as HTMLInputElement).value }
                                    attrs.minLength = "8"
                                    attrs.maxLength = "50"
                                }
                                label {
                                    attrs.htmlFor = "password"
                                    +"Password"
                                }
                            }
                        }
                        div("row center") {
                            button(classes = "waves-effect waves-light btn", type = ButtonType.submit) {
                                attrs.disabled = !buttonEnabled
                                +"Create Account"
                            }
                        }
                    }
                }
            }
        }
    }
}