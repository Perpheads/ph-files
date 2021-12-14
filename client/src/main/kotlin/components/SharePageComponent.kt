package com.perpheads.files.components

import com.perpheads.files.WebSocketSender
import com.perpheads.files.logoutIfUnauthorized
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import org.w3c.files.File
import org.w3c.files.get
import react.Props
import react.dom.div
import react.dom.onClick
import react.dom.onDragOver
import react.dom.onDrop
import react.fc
import react.router.useNavigate
import react.useEffectOnce
import react.useState
import styled.*

val ShareComponent = fc<Props>("ShareComponent") {
    val navigate = useNavigate()
    var dropEnabled by useState(true)
    var droppedFile by useState<File>()
    var downloadProgress by useState<Long>()
    var createdLink by useState<String>()
    var webSocketSender by useState<WebSocketSender>()
    var completed by useState(false)
    var error by useState(false)

    useEffectOnce {
        MainScope().launch {
            logoutIfUnauthorized(navigate) { }
        }
        cleanup {
            webSocketSender?.close()
        }
    }

    fun createSender(file: File) {
        val sender = WebSocketSender("/share/ws", file)
        sender.onLinkCreated = {
            createdLink = "${window.location.origin}/#/share/$it"
        }
        sender.onProgress = { downloadProgress = it }
        sender.onCompleted = { completed = true }
        sender.onError = { error = true }

        webSocketSender = sender
        sender.open()
    }

    div {
        navBar {
            message = "Share Files"
            showSearchBar = false
            onSearchChanged = {}
        }
        div("container") {
            styledDiv {
                css {
                    classes += "card fadeIn animated"
                    paddingBottom = 18.px
                    height = 100.pct
                    paddingTop = 10.px
                    paddingRight = 10.px
                    paddingLeft = 10.px
                }
                div("col l4 center-align") {
                    styledDiv {
                        css {
                            minHeight = 200.px
                        }
                        +"Drop a File here"
                        this.attrs.onDragOver = {
                            it.preventDefault()
                        }
                        this.attrs.onDrop = {
                            it.preventDefault()
                            if (dropEnabled) {
                                droppedFile = it.dataTransfer.files[0]
                            }
                        }
                    }
                    val currentLink = createdLink
                    val currentProgress = downloadProgress
                    if (dropEnabled) {
                        styledButton {
                            css {
                                classes += "btn waves-effect waves-light"
                                if (droppedFile == null || !dropEnabled) {
                                    classes += "disabled"
                                }
                            }
                            attrs.onClick = {
                                val file = droppedFile
                                if (file != null && dropEnabled) {
                                    dropEnabled = false
                                    createSender(file)
                                    console.log("Uploading file $file")
                                }
                            }

                            +"Create Link"
                        }
                    } else if (currentLink != null && currentProgress == null) {
                        styledInput {
                            attrs.disabled = true
                            css {
                            }
                            attrs.value = currentLink
                        }
                    } else if (currentProgress != null) {
                        //TODO:
                    }
                }
            }
        }
    }
}
