package com.perpheads.files.components

import com.perpheads.files.ApiClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import react.router.dom.redirect
import react.router.dom.routeLink
import react.router.dom.useHistory
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
    val history = useHistory()

    useEffectOnce {
        ApiClient.mainScope.launch {
            if (ApiClient.getLoggedIn()) {
                history.replace("/account")
            }
        }
    }

    fun login() {
        props.setError(null)
        ApiClient.mainScope.launch {
            try {
                val response = ApiClient.authenticate(username, password, remember)
                props.setError(response.error)
                if (response.error == null) {
                    history.replace("/account")
                }
            } catch (e: Exception) {
                props.setError("An unknown error occurred")
            }
        }
    }

    val onEnterPressed: KeyboardEventHandler<*> = { event ->
        if (event.key == "Enter") {
            login()
        }
    }

    div("valign-wrapper row log-in") {
        div("col card hoverable s10 pull-s1 m6 pull-m3 l4 pull-l4 shake animated") {
            div("card-content") {
                span("card-title") {
                    +"Enter your details"
                }
                div("row") {
                    div("input-field col s11") {
                        input(type = InputType.text) {
                            attrs.placeholder = "Username"
                            attrs.onChangeFunction = { event ->
                                setUsername((event.target as HTMLInputElement).value)
                            }
                            attrs.onKeyPress = onEnterPressed
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
                            attrs.onKeyPress = onEnterPressed
                        }
                    }
                    div("center-align") {
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

            div("card-action right-align") {
                button(classes = "btn waves-effect waves-light") {
                    +"Login"
                    attrs.onClick = { login() }
                }
            }
        }
    }
}


val LoginPageComponent = functionComponent<LoginComponentProps>("LoginComponent") { _ ->
    val (error, errorSet) = useState<String?>(null)
    div {
        div("center-align") {
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
        loginCardComponent { setError = errorSet }
    }
}

fun RBuilder.loginCardComponent(handler: LoginCardComponentProps.() -> Unit) = child(LoginCardComponent) {
    attrs { handler() }
}

fun RBuilder.loginPageComponent(handler: LoginComponentProps.() -> Unit) = child(LoginPageComponent) {
    attrs { handler() }
}