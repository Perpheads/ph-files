package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.logoutIfUnauthorized
import csstype.*
import js.core.jso
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.icons.material.ContentCopy
import mui.icons.material.Refresh
import mui.material.*
import mui.material.styles.Theme
import mui.material.styles.useTheme
import mui.system.Breakpoint
import mui.system.sx
import react.*
import react.router.useNavigate

val ApiKeyComponent = fc<Props>("ApiKeyComponent") {
    var apiKey by useState("Loading")
    val navigate = useNavigate()

    val theme = useTheme<Theme>()
    val smallScreen = useMediaQuery(theme.breakpoints.down(Breakpoint.md))

    var alertColor by useState(AlertColor.info)
    var alertText by useState<String?>(null)

    useEffectOnce {
        MainScope().launch {
            logoutIfUnauthorized(navigate) {
                apiKey = ApiClient.getApiKey().apiKey
            }
        }
    }

    fun generateApiKey() {
        MainScope().launch {
            logoutIfUnauthorized(navigate) {
                apiKey = ApiClient.generateApiKey().apiKey
                alertColor = AlertColor.success
                alertText = "New API key generated successfully"
            }
        }
    }

    fun copyShareXConfig() {
        val location = window.location.origin
        window.navigator.clipboard.writeText(
            """
                {
                  "Version": "14.0.1",
                  "DestinationType": "ImageUploader, TextUploader, FileUploader",
                  "RequestMethod": "POST",
                  "RequestURL": "$location/upload",
                  "Headers": {
                    "API-KEY": "$apiKey"
                  },
                  "Body": "MultipartFormData",
                  "FileFormName": "file",
                  "URL": "$location/{json:link}"
                }
            """.trimIndent()
        )
        alertColor = AlertColor.info
        alertText = "ShareX template copied to clipboard"
    }

    DialogTitle {
        +"API Key"
    }
    Divider { }
    DialogContent {
        TextField {
            attrs {
                fullWidth = true
                inputProps = jso<InputBaseProps> {
                    this.readOnly = true
                }.asDynamic() as? InputBaseComponentProps
                label = ReactNode("API Key")
                onFocus = {
                    window.navigator.clipboard.writeText(apiKey)
                    alertColor = AlertColor.info
                    alertText = "API Key copied successfully"
                }

                value = apiKey
            }
        }
        Box {
            attrs.sx {
                display = Display.flex
                flexDirection = if (smallScreen) {
                    FlexDirection.column
                } else {
                    FlexDirection.row
                }
                gap = 10.px
                justifyContent = JustifyContent.center
                alignItems = AlignItems.center
                marginTop = 32.px
                marginRight = 16.px
                marginLeft = 16.px
            }

            Button {
                attrs.color = ButtonColor.warning
                attrs.variant = ButtonVariant.contained
                attrs.endIcon = Refresh.create()
                attrs.onClick = { generateApiKey() }
                attrs.sx {
                    if (smallScreen) {
                        width = 100.pct
                    }
                }
                +"Regenerate API Key"
            }
            Button {
                attrs.color = ButtonColor.info
                attrs.variant = ButtonVariant.contained
                attrs.endIcon = ContentCopy.create()
                attrs.sx {
                    if (smallScreen) {
                        width = 100.pct
                    }
                }
                attrs.onClick = { copyShareXConfig() }

                +"Copy ShareX Config"
            }
        }
    }

    Snackbar {
        attrs {
            open = alertText != null
            autoHideDuration = 6000
            onClose = { _, _ ->
                alertText = null
            }
        }
        alertText?.let { text ->
            Alert {
                attrs.onClose = {
                    alertText = null
                }
                attrs.sx {
                    width = 100.pct
                }
                attrs.severity = alertColor
                +text
            }
        }
    }
}