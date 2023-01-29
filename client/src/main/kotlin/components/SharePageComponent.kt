package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.WebSocketSender
import com.perpheads.files.data.ShareFileResponse
import com.perpheads.files.logoutIfUnauthorized
import com.perpheads.files.useScope
import csstype.*
import js.core.get
import js.core.jso
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.css.display
import kotlinx.html.id
import mui.icons.material.Share
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import org.w3c.dom.BeforeUnloadEvent
import react.*
import react.dom.onChange
import react.router.useNavigate
import styled.css
import styled.styledInput
import web.dom.document
import web.file.File
import web.html.HTMLInputElement
import web.html.InputType

private val preventPageClose: (BeforeUnloadEvent) -> String? = {
    it.preventDefault()
    "There are file transfers in progress, are you sure you want to close this page?"
}

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
    var currentAlert by useState<AlertData?>(null)
    val scope = useScope()

    useEffect(createdLink, completed) {
        if (!completed && createdLink != null) {
            window.onbeforeunload = preventPageClose
        } else {
            window.onbeforeunload = null
        }

        cleanup {
            window.onbeforeunload = null
        }
    }

    useEffectOnce {
        scope.launch {
            logoutIfUnauthorized(navigate) {
                ApiClient.getAccountInfo()
            }
        }
        cleanup {
            webSocketSender?.close()
        }
    }

    fun createSender(file: File) {
        val sender = WebSocketSender("/share/ws", file, scope)
        sender.onLinkCreated = {
            createdLink = "${window.location.origin}/#/share/$it"
        }
        sender.onProgress = { downloadProgress = it }
        sender.onCompleted = { completed = true }
        sender.onError = { error = it }

        webSocketSender = sender
        sender.open()
    }

    Page {
        attrs.name = "Share Files"
        attrs.searchBarEnabled = false
        attrs.currentAlert = currentAlert
        attrs.onAlertHidden = {
            currentAlert = null
        }

        val currentLink = createdLink
        val currentProgress = downloadProgress
        val file = droppedFile


        if (currentLink == null) {
            styledInput {
                css {
                    display = kotlinx.css.Display.none
                }
                attrs.type = kotlinx.html.InputType.file
                attrs.id = "file-input"
                attrs.onChange = { event ->
                    (event.target as HTMLInputElement).files?.let { inputFiles ->
                        if (dropEnabled && inputFiles.length > 0) {
                            droppedFile = inputFiles[0]
                        }
                    }
                }
            }

            Paper {
                attrs.sx {
                    width = 100.pct
                    height = 300.px
                    display = Display.flex
                    alignItems = AlignItems.center
                    justifyContent = JustifyContent.center
                    flexDirection = FlexDirection.row
                    cursor = Cursor.pointer
                }
                attrs.elevation = if (dropZoneHovered) 8 else 2

                attrs.onClick = {
                    it.stopPropagation()
                    it.preventDefault()
                    document.getElementById("file-input")?.let { elem ->
                        (elem as HTMLInputElement).click()
                    }
                }

                attrs.onDragLeave = {
                    dropZoneHovered = false
                }
                attrs.onDragOver = {
                    dropZoneHovered = true
                    it.preventDefault()
                }
                attrs.onDrop = {
                    dropZoneHovered = false
                    it.preventDefault()
                    if (dropEnabled) {
                        droppedFile = it.dataTransfer.files[0]
                    }
                }

                Typography {
                    attrs.sx {
                        userSelect = "none".unsafeCast<UserSelect>()
                    }
                    attrs.variant = TypographyVariant.h5
                    +"Drop File Here"
                }
            }
        }

        if (file != null) {
            SharePreviewComponent {
                attrs.file = ShareFileResponse(file.name, file.size.toLong())
            }
        }

        if (dropEnabled) {
            Button {
                attrs.sx {
                    marginTop = 16.px
                }
                attrs.disabled = file == null
                attrs.color = ButtonColor.secondary
                attrs.variant = ButtonVariant.contained
                attrs.endIcon = Share.create()
                attrs.onClick = {
                    if (file != null && dropEnabled) {
                        dropEnabled = false
                        createSender(file)
                    }
                }
                +"Create Link"
            }
        } else if (currentLink != null && currentProgress == null) {
            Typography {
                attrs.variant = TypographyVariant.body1
                +"Send this link to whoever should download this file."
            }

            TextField {
                attrs {
                    fullWidth = true
                    inputProps = jso<InputBaseProps> {
                        this.readOnly = true
                    }.asDynamic() as? InputBaseComponentProps
                    label = ReactNode("Share Link")
                    onClick = {
                        it.stopPropagation()
                        it.preventDefault()
                    }
                    onFocus = {
                        it.stopPropagation()
                        it.preventDefault()
                        scope.launch {
                            currentAlert = try {
                                window.navigator.clipboard.writeText(currentLink).await()
                                AlertData("Share link copied successfully", AlertColor.info)
                            } catch (e: Exception) {
                                AlertData("Failed to copy share link", AlertColor.error)
                            }
                        }
                    }
                    value = currentLink
                }
            }
        }

        if (currentProgress != null && file != null) {
            Typography {
                attrs.variant = TypographyVariant.h5
                attrs.sx {
                    marginTop = 20.px
                }
                if (completed) {
                    +"File upload completed"
                } else {
                    +"File upload in progress"
                }
            }
            Typography {
                attrs.variant = TypographyVariant.body1
                attrs.sx {
                    marginTop = 3.px
                }
                +"Note: This window needs to stay open for the file transfer to complete."
            }
            val percentage = (currentProgress.toDouble() * 100 / file.size)
            LinearProgress {
                attrs.variant = LinearProgressVariant.determinate
                attrs.sx {
                    width = 100.pct
                    marginTop = 4.px
                }
            }

            val displayPercentage = percentage.asDynamic().toFixed(1).unsafeCast<String>()
            Typography {
                attrs.variant = TypographyVariant.body1
                attrs.sx {
                    marginTop = 10.px
                }
                if (completed) {
                    +"Completed"
                } else {
                    +"Upload Progress: $displayPercentage%"
                }
            }
        }
    }
}
