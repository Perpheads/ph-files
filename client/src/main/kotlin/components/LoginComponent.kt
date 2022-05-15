package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.replace
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import react.dom.events.KeyboardEventHandler
import react.router.Navigate
import react.router.useNavigate
import styled.css
import styled.styledDiv
import styled.styledImg
import styled.styledInput

external interface LoginCardComponentProps : Props {
    var setError: StateSetter<String?>
}

val LoginCardComponent = fc<LoginCardComponentProps>("LoginCardComponent") { props ->
    val (username, setUsername) = useState("")
    val (password, setPassword) = useState("")
    val (remember, setRemember) = useState(false)
    val navigate = useNavigate()

    useEffectOnce {
        ApiClient.mainScope.launch {
            if (ApiClient.getLoggedIn()) {
                window.localStorage.setItem("loggedIn", "yes")
                navigate.replace("/account")
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
                    window.localStorage.setItem("loggedIn", "yes")
                    navigate.replace("/account")
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
                    div("input-field col s11") {
                        input(type = InputType.password, classes = "validate") {
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


val LoginPageComponent = fc<Props>("LoginComponent") { _ ->
    val (error, errorSet) = useState<String?>(null)
    if (window.localStorage.getItem("loggedIn") == "yes") {
        Navigate {
            attrs.to = "/account"
        }
    }
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