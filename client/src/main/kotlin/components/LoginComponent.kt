package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.replace
import csstype.*
import js.core.jso
import kotlinx.browser.window
import kotlinx.coroutines.launch
import mui.icons.material.CheckBox
import mui.material.*
import mui.material.styles.Theme
import mui.material.styles.TypographyVariant
import mui.material.styles.createTheme
import mui.material.styles.useTheme
import mui.system.ThemeOptions
import mui.system.ThemeProvider
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.div
import react.dom.events.KeyboardEventHandler
import react.dom.html.ButtonType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.main
import react.dom.img
import react.dom.onChange
import react.router.Navigate
import react.router.useNavigate
import web.html.HTMLInputElement
import web.html.InputType

external interface LoginCardComponentProps : Props {
}

val LoginCardComponent = fc<LoginCardComponentProps>("LoginCardComponent") {
    var error by useState<String?>(null)
    var username by useState("")
    var password by useState("")
    var remember by useState(false)
    val theme = useTheme<Theme>()
    val navigate = useNavigate()

    val logoPath = if (theme.palette.mode == PaletteMode.dark) "/logo-dark.png" else "logo.png"


    useEffectOnce {
        ApiClient.mainScope.launch {
            if (ApiClient.getLoggedIn()) {
                window.localStorage.setItem("loggedIn", "yes")
                navigate.replace("/account")
            }
        }
    }

    fun login() {
        error = null
        ApiClient.mainScope.launch {
            try {
                val response = ApiClient.authenticate(username, password, remember)
                error = response.error
                if (response.error == null) {
                    window.localStorage.setItem("loggedIn", "yes")
                    navigate.replace("/account")
                }
            } catch (e: Exception) {
                error = "An unknown error occurred"
            }
        }
    }

    Box {
        attrs.sx {
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
        }

        Box {
            attrs.sx {
                marginTop = 100.px
                marginBottom = 50.px
            }
            img {
                attrs.src = ApiClient.getLocalLink(logoPath)
            }
        }

        Paper {
            attrs {
                sx {
                    padding = 24.px
                    display = Display.flex
                    alignItems = AlignItems.center
                    flexDirection = FlexDirection.column
                }
            }

            Typography {
                attrs.component = h1
                attrs.variant = TypographyVariant.h4
                +"Enter your details"
            }

            Box {
                attrs.component = form
                attrs.sx {
                    marginTop = 2.px
                }
                attrs.onSubmit = {
                    it.preventDefault()
                    login()
                }

                TextField {
                    attrs {
                        margin = FormControlMargin.normal
                        required = true
                        fullWidth = true
                        name = "username"
                        label = ReactNode("Username")
                        onChange = {
                            username = (it.target as HTMLInputElement).value
                        }
                    }
                }

                TextField {
                    attrs {
                        margin = FormControlMargin.normal
                        required = true
                        fullWidth = true
                        type = InputType.password
                        name = "password"
                        label = ReactNode("Password")
                        onChange = {
                            password = (it.target as HTMLInputElement).value
                        }
                    }
                }

                FormControlLabel {
                    attrs.label = ReactNode("Remember me")
                    attrs.control = Checkbox.create {
                        value = "remember"
                        checked = remember
                        onChange = { _, checked ->
                            remember = checked
                        }
                    }
                }
                FormHelperText {
                    attrs.error = error != null
                    +(error ?: " ")
                }

                Button {
                    attrs {
                        type = ButtonType.submit
                        fullWidth = true
                        variant = ButtonVariant.contained
                        sx {
                            marginTop = 3.px
                            marginBottom = 2.px
                        }
                    }
                    +"Sign In"
                }
            }
        }
    }
}


val LoginPageComponent = fc<Props>("LoginComponent") { _ ->
    val navigate = useNavigate()

    val paletteMode = if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
        PaletteMode.dark
    }  else {
        PaletteMode.light
    }

    val theme = createTheme(jso {
        palette = jso {
            mode = paletteMode
        }
    })

    if (window.localStorage.getItem("loggedIn") == "yes") {
        Navigate {
            attrs.to = "/account"
        }
    }
    ThemeProvider {
        attrs.theme = theme
        Container {
            attrs.component = main
            attrs.maxWidth = "sm"

            CssBaseline { }

            LoginCardComponent { }
        }
        Link {
            attrs {
                variant = ReactHTML.h5
                underline = LinkUnderline.none
                href = "#"
                sx {
                    position = Position.absolute
                    right = 15.px
                    bottom = 15.px
                }
                onClick = {
                    it.preventDefault()
                    navigate.replace("/contact")
                }
            }
            +"Contact"
        }
    }
}