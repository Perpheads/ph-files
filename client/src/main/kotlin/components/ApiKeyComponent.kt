package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.logoutIfUnauthorized
import com.perpheads.files.showToast
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import react.Props
import react.dom.*
import react.fc
import react.router.useNavigate
import react.useEffectOnce
import react.useState
import styled.*

val ApiKeyComponent = fc<Props>("ApiKeyComponent") {
    var apiKey by useState("Loading")
    val navigate = useNavigate()

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
                showToast("New API key generated successfully")
            }
        }
    }

    fun copyShareXConfig() {
        window.navigator.clipboard.writeText(
            """
                {
                  "Version": "14.0.1",
                  "DestinationType": "ImageUploader, TextUploader, FileUploader",
                  "RequestMethod": "POST",
                  "RequestURL": "https://files.perpheads.com/upload",
                  "Headers": {
                    "API-KEY": "$apiKey"
                  },
                  "Body": "MultipartFormData",
                  "FileFormName": "file",
                  "URL": "https://files.perpheads.com/{json:link}"
                }
            """.trimIndent()
        )
        showToast("ShareX template copied to clipboard")
    }

    div {
        navBar {
            message = "API Key"
            showSearchBar = false
            onSearchChanged = {}
        }

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
                    div("row") {
                        div("input-field col s11") {
                            styledInput {
                                attrs.id = "api_key_input"
                                attrs.disabled = true
                                attrs.value = apiKey
                                attrs.type = InputType.text
                            }
                            label {
                                attrs.htmlFor = "api_key_input"
                                attrs.classes += "active"
                                +"API Key: "
                            }
                        }
                        div("col s1") {
                            button(classes = "waves-effect waves-light btn") {
                                attrs.onClick = {
                                    window.navigator.clipboard.writeText(apiKey)
                                    showToast("API key copied to clipboard")
                                }
                                i("material-icons") {
                                    +"content_copy"
                                }
                            }
                        }
                    }
                    styledDiv {
                        styledButton {
                            css {
                                classes += "btn waves-effect waves-light orange"
                                marginRight = 8.px
                            }
                            attrs.onClickFunction = { generateApiKey() }
                            +"Regenerate API Key"
                            i("material-icons right") {
                                +"refresh"
                            }
                        }
                        styledButton {
                            css {
                                classes += "btn waves-effect waves-light"
                                marginLeft = 8.px
                            }
                            attrs.onClickFunction = { copyShareXConfig() }
                            +"Copy ShareX Uploader Config"
                            i("material-icons right") {
                                +"content_copy"
                            }
                        }
                    }
                }
            }
        }
    }
}