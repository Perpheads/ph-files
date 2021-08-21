package com.perpheads.files.components

import com.perpheads.files.ApiClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.StateSetter
import react.dom.label
import react.dom.onClick
import react.dom.span
import react.functionComponent
import react.useState
import styled.*


external interface LoginComponentProps : RProps {
}

external interface LoginCardComponentProps : RProps {
    var setError: StateSetter<String?>
}

val LoginCardComponent = functionComponent<LoginCardComponentProps>("LoginCardComponent") { props ->
    val (username, setUsername) = useState("")
    val (password, setPassword) = useState("")
    val (remember, setRemember) = useState(false)

    fun login() {
        props.setError(null)
        MainScope().launch {
            ApiClient.authenticate(username, password, remember)
        }
    }

    styledDiv {
        css { classes += "valign-wrapper row log-in" }
        styledDiv {
            css { classes += "col card hoverable s10 pull-s1 m6 pull-m3 l4 pull-l4 shake animated" }
            styledDiv {
                css { classes += "card-content" }
                styledSpan {
                    css { classes += "card-title" }
                    +"Enter your details"
                }
                styledDiv {
                    css { classes += "row" }
                    styledDiv {
                        css { classes += "input-field col s11" }
                        styledInput(type = InputType.text) {
                            attrs.placeholder = "Username"
                            attrs.onChangeFunction = { event ->
                                setUsername((event.target as HTMLInputElement).value)
                            }
                        }
                    }
                    styledDiv {
                        css { classes += "input-field col s11" }
                        styledInput(type = InputType.password) {
                            css { classes += "validate" }
                            attrs.placeholder = "Password"
                            attrs.onChangeFunction = { event ->
                                setPassword((event.target as HTMLInputElement).value)
                            }
                        }
                    }
                    styledDiv {
                        css { classes += "center-align" }
                        label {
                            styledInput(type = InputType.checkBox) {
                                attrs.onChangeFunction = { event ->
                                    setRemember((event.target as HTMLInputElement).checked)
                                }
                            }
                            span { +"Remember Me" }
                        }
                    }
                }
            }

            styledDiv {
                css { classes += "card-action right-align" }
                styledButton {
                    css { classes += "btn waves-effect waves-light" }
                    +"Login"
                    attrs.onClick = { login() }
                }
            }
        }
    }
}


val LoginPageComponent = functionComponent<LoginComponentProps>("LoginComponent") { _ ->
    val (error, errorSet) = useState<String?>(null)
    styledDiv {
        styledDiv {
            css { classes += "center-align" }
            styledImg(src = "/logo.png") {
                css {
                    marginBottom = 100.px
                    marginTop = 100.px
                }
            }
        }
        if (error != null) {
            styledDiv {
                css {
                    classes += "center-align"
                    color = Color.red
                    alignSelf = Align.center
                }
                +error
            }
        }
        child(LoginCardComponent) {
            attrs.setError = errorSet
        }
    }
}
