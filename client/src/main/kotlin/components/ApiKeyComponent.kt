package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.logoutIfUnauthorized
import com.perpheads.files.useScope
import csstype.*
import js.core.jso
import kotlinx.browser.window
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

external interface ApiKeyProps: Props {
    var showAlert: (String, AlertColor) -> Unit
}

val ApiKeyComponent = fc<ApiKeyProps>("ApiKeyComponent") { props ->
    var apiKey by useState("Loading")
    val navigate = useNavigate()

    val theme = useTheme<Theme>()
    val smallScreen = useMediaQuery(theme.breakpoints.down(Breakpoint.md))
    val scope = useScope()

    useEffectOnce {
        scope.launch {
            logoutIfUnauthorized(navigate) {
                apiKey = ApiClient.getApiKey().apiKey
            }
        }
    }

    fun generateApiKey() {
        scope.launch {
            logoutIfUnauthorized(navigate) {
                apiKey = ApiClient.generateApiKey().apiKey
                props.showAlert("New API key generated successfully", AlertColor.success)
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
        props.showAlert("ShareX template copied to clipboard", AlertColor.info)
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
                onClick = {
                    it.stopPropagation()
                    it.preventDefault()
                }
                onFocus = {
                    it.stopPropagation()
                    it.preventDefault()
                    window.navigator.clipboard.writeText(apiKey)
                    props.showAlert("API Key copied successfully", AlertColor.info)
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
}