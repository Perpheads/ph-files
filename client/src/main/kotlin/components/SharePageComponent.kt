package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.WebSocketSender
import com.perpheads.files.logoutIfUnauthorized
import com.perpheads.files.data.ShareFileResponse
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import js.core.get
import react.Props
import react.dom.*
import react.fc
import react.router.useNavigate
import react.useEffectOnce
import react.useState
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledInput
import web.file.File

val ShareComponent = fc<Props>("ShareComponent") {
    val navigate = useNavigate()
    var dropEnabled by useState(true)
    var droppedFile by useState<File>()
    var downloadProgress by useState<Long>()
    var createdLink by useState<String>()
    var webSocketSender by useState<WebSocketSender>()
    var completed by useState(false)
    var error by useState<String>()
    var dropZoneHovered by useState(false)

    useEffectOnce {
        MainScope().launch {
            logoutIfUnauthorized(navigate) {
                ApiClient.getAccountInfo()
            }
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
                                classes += "card"
                                marginBottom = 10.px
                                paddingBottom = 10.px
                                paddingTop = 10.px
                                paddingRight = 10.px
                                paddingLeft = 10.px
                            }
                            styledDiv {
                                css {
                                    minHeight = 200.px
                                    if (dropZoneHovered) {
                                        this.put("outline", "dashed red")
                                    } else {
                                        this.put("outline", "dashed green")
                                    }
                                }
                                +"Drop a File here"
                                this.attrs.onDragLeave = {
                                    dropZoneHovered = false
                                }
                                this.attrs.onDragOver = {
                                    dropZoneHovered = true
                                    it.preventDefault()
                                }
                                this.attrs.onDrop = {
                                    dropZoneHovered = false
                                    it.preventDefault()
                                    if (dropEnabled) {
                                        droppedFile = it.dataTransfer.files[0]
                                    }
                                }
                            }
                        }
                    }
                    if (file != null) {
                        SharePreviewComponent {
                            attrs.file = ShareFileResponse(file.name, file.size.toLong())
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
                                }
                            }

                            +"Create Link"
                        }
                    } else if (currentLink != null && currentProgress == null) {
                        div {
                            p {
                                +"Send this link to whoever should download this file."
                            }
                            div("row") {
                                div("col s11") {
                                    styledInput {
                                        attrs.disabled = true
                                        css {
                                        }
                                        attrs.value = currentLink
                                    }
                                }
                                div("col s1") {
                                    button(classes = "waves-effect waves-light btn") {
                                        attrs.onClick = {
                                            window.navigator.clipboard.writeText(createdLink ?: "")
                                        }
                                        i("material-icons") {
                                            +"content_copy"
                                        }
                                    }
                                }
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
                            val percentage = (currentProgress.toDouble() * 100 / file.size.toDouble())
                            div("progress") {
                                styledDiv {
                                    css {
                                        classes += "determinate"
                                        width = percentage.pct
                                    }
                                }
                            }
                            p {
                                val displayPercentage = percentage.asDynamic().toFixed(1).unsafeCast<String>()
                                +"Upload Progress: $displayPercentage%"
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
