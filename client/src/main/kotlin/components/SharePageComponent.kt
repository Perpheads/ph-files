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
import react.dom.*
import react.fc
import react.router.useNavigate
import react.useEffectOnce
import react.useState
import styled.*
import kotlin.math.roundToInt

val ShareComponent = fc<Props>("ShareComponent") {
    val navigate = useNavigate()
    var dropEnabled by useState(true)
    var droppedFile by useState<File>()
    var downloadProgress by useState<Long>()
    var createdLink by useState<String>()
    var webSocketSender by useState<WebSocketSender>()
    var completed by useState(false)
    var error by useState<String>()

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
        sender.onError = { error = it }

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
                    error?.let {
                        styledDiv {
                            css {
                                color = Color.red
                            }
                            +"Error: $it"
                        }
                    }
                    val currentLink = createdLink
                    val currentProgress = downloadProgress
                    val file = droppedFile
                    if (currentLink == null) {
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
                    }
                    if (dropEnabled) {
                        styledButton {
                            css {
                                classes += "btn waves-effect waves-light"
                                if (droppedFile == null || !dropEnabled) {
                                    classes += "disabled"
                                }
                            }
                            attrs.onClick = {
                                if (file != null && dropEnabled) {
                                    dropEnabled = false
                                    createSender(file)
                                    console.log("Uploading file $file")
                                }
                            }

                            +"Create Link"
                        }
                    } else if (currentLink != null && currentProgress == null) {
                        div {
                            p {
                                +"Send this link to whoever should download this file."
                            }
                            styledInput {
                                attrs.disabled = true
                                css {
                                }
                                attrs.value = currentLink
                            }
                        }
                    }
                    if (currentProgress != null && file != null) {
                        div {
                            p {
                                +"File upload in progress"
                            }
                            p {
                                +"Note: This window needs to stay open for the file transfer to complete."
                            }
                            div("progress") {
                                styledDiv {
                                    css {
                                        classes += "determinate"
                                        width = (currentProgress.toDouble() * 100 / file.size.toDouble()).pct
                                    }
                                }
                            }
                        }
                        if (completed) {
                            p {
                                +"Upload Completed!"
                            }
                        }
                    }
                }
            }
        }
    }
}
