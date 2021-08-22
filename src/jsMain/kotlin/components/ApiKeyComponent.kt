package com.perpheads.files.components

import com.perpheads.files.ApiClient
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.button
import react.dom.div
import react.router.dom.useHistory
import styled.*

val ApiKeyComponent = fc<RProps>("ApiKeyComponent") {
    val (apiKey, setApiKey) = useState("Loading")
    val history = useHistory()

    useEffectOnce {
        MainScope().launch {
            try {
                setApiKey(ApiClient.getApiKey().apiKey)
            } catch (_: ApiClient.UnauthorizedException) {
                history.replace("/")
            }
        }
    }

    fun generateApiKey() {
        MainScope().launch {
            try {
                setApiKey(ApiClient.generateApiKey().apiKey)
            } catch (_: ApiClient.UnauthorizedException) {
                history.replace("/")
            }
        }
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
                    div("input-field col s8") {
                        styledP {
                            css {
                                classes += "flow-text"
                                fontSize = 17.px
                            }
                            +"API Key: $apiKey"
                        }
                    }
                    button(classes = "btn waves-effect waves-light") {
                        attrs.onClickFunction = { generateApiKey() }
                        +"Regenerate API Key"
                    }
                }
            }
        }
    }
}

fun RBuilder.apiKeyPage() = child(ApiKeyComponent)