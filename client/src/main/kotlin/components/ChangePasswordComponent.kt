package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.logout
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import org.w3c.dom.HTMLInputElement
import react.Props
import react.dom.*
import react.dom.events.KeyboardEventHandler
import react.fc
import react.router.useNavigate
import react.useState
import styled.*

val ChangePasswordComponent = fc<Props>("ChangePasswordComponent") {
    val navigate = useNavigate()
    val (password, setPassword) = useState("")
    val (newPassword, setNewPassword) = useState("")
    val (repeatNewPassword, setRepeatNewPassword) = useState("")
    val (error, setError) = useState("")

    fun changePassword() {
        if (password.isEmpty()) {
            setError("Existing password is empty")
        } else if (newPassword.length < 6) {
            setError("New password must be at least 6 characters")
        } else if (repeatNewPassword != newPassword) {
            setError("New password does not match repeated password")
        } else {
            ApiClient.mainScope.launch {
                ApiClient.changePassword(password, newPassword)
                logout(navigate)
            }
        }
    }

    val onEnterPressed: KeyboardEventHandler<*> = { event ->
        if (event.key == "Enter") {
            changePassword()
        }
    }

    div {
        navBar { message = "Change Password" }

        div("container") {
            styledDiv {
                css {
                    classes += "card fadeIn animated"
                    padding(10.px)
                    paddingBottom = 18.px
                    height = 100.pct
                    minHeight = 100.pct
                }
                div("col l4 center-align") {
                    if (error.isEmpty()) {
                            styledDiv {
                            css {
                                classes += "center-align"
                                color = Color.red
                            }
                            +error
                        }
                    }
                    div("input-field col s8") {
                        styledP {
                            css {
                                classes += "flow-text"
                                fontSize = 15.px
                            }
                            +"Existing Password"
                        }
                        input(InputType.password) {
                            attrs {
                                placeholder = "Existing Password"
                                onInput = { event ->
                                    setPassword((event.target as HTMLInputElement).value)
                                }
                                onKeyPress = onEnterPressed
                            }
                        }
                    }
                    div("input-field col s8") {
                        styledP {
                            css {
                                classes += "flow-text"
                                fontSize = 15.px
                            }
                            +"New Password"
                        }
                        input(InputType.password) {
                            attrs {
                                placeholder = "New Password"
                                onInput = { event ->
                                    setNewPassword((event.target as HTMLInputElement).value)
                                }
                                onKeyPress = onEnterPressed
                            }
                        }
                    }
                    div("input-field col s8") {
                        styledP {
                            css {
                                classes += "flow-text"
                                fontSize = 15.px
                            }
                            +"Repeat Password"
                        }
                        input(InputType.password) {
                            attrs {
                                placeholder = "Repeat Password"
                                onInput = { event ->
                                    setRepeatNewPassword((event.target as HTMLInputElement).value)
                                }
                                onKeyPress = onEnterPressed
                            }
                        }
                    }
                    button(classes = "btn waves-effect waves-light") {
                        attrs.onClick = { changePassword() }
                        +"Change Password"
                    }
                }
            }
        }
    }
}
