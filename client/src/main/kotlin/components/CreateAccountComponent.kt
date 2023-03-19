package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.replace
import com.perpheads.files.useAccount
import com.perpheads.files.useScope
import csstype.px
import kotlinx.coroutines.launch
import mui.material.*
import mui.system.sx
import react.*
import react.dom.html.ReactHTML
import react.dom.onChange
import react.router.useNavigate
import web.html.ButtonType

external interface CreateAccountProps : Props {
    var showAlert: (String, AlertColor) -> Unit
    var onDialogClosed: () -> Unit
}

val CreateAccountComponent = fc<CreateAccountProps>("CreateAccountComponent") {props ->
    val (user, _) = useAccount()
    val navigate = useNavigate()
    var username by useState("")
    var email by useState("")
    var password by useState("")
    var buttonEnabled by useState(true)
    val scope = useScope()

    useEffect(user) {
        if (user != null && !user.admin) {
            navigate.replace("/")
        }
    }


    DialogTitle {
        +"Create New Account"
    }
    Divider { }
    DialogContent {
        Box {
            attrs.component = ReactHTML.form
            attrs.sx {
                marginTop = 2.px
            }
            attrs.onSubmit = {
                it.preventDefault()
                buttonEnabled = false
                scope.launch {
                    try {
                        ApiClient.createUser(username, email, password)
                        username = ""
                        email = ""
                        password = ""
                        props.showAlert("New account created successfully", AlertColor.success)
                        props.onDialogClosed()
                    } catch (e: Exception) {
                        props.showAlert("Failed creating new account", AlertColor.error)
                    }
                    buttonEnabled = true
                }
            }

            TextField {
                attrs {
                    margin = FormControlMargin.normal
                    required = true
                    fullWidth = true
                    name = "username"
                    label = ReactNode("Username")
                    onChange = {
                        username = (it.target as web.html.HTMLInputElement).value
                    }
                }
            }

            TextField {
                attrs {
                    margin = FormControlMargin.normal
                    required = true
                    fullWidth = true
                    name = "email"
                    type = web.html.InputType.email
                    label = ReactNode("Email")
                    onChange = {
                        email = (it.target as web.html.HTMLInputElement).value
                    }
                }
            }

            TextField {
                attrs {
                    margin = FormControlMargin.normal
                    required = true
                    fullWidth = true
                    type = web.html.InputType.password
                    name = "password"
                    label = ReactNode("Password")
                    onChange = {
                        password = (it.target as web.html.HTMLInputElement).value
                    }
                }
            }

            Button {
                attrs {
                    type = ButtonType.submit
                    fullWidth = true
                    variant = ButtonVariant.contained
                    sx {
                        marginTop = 12.px
                        marginBottom = 2.px
                    }
                }
                +"Create Account"
            }
        }
    }
}