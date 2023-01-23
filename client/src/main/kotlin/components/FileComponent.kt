package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.FileResponse
import com.perpheads.files.data.validateFilename
import csstype.*
import mui.icons.material.DeleteOutline
import mui.icons.material.Edit
import mui.icons.material.MoreVert
import mui.material.*
import mui.material.Size
import mui.system.sx
import react.*
import react.dom.onChange
import web.html.HTMLInputElement
import web.window.WindowTarget

external interface FileComponentProps : Props {
    var file: FileResponse
    var deleteFile: (FileResponse) -> Unit
    var renameFile: (FileResponse, String) -> Unit
}

val FileComponent = fc<FileComponentProps>("FileComponent") { props ->
    var editing by useState(false)
    var invalidFileName by useState(false)
    val extension = "." + props.file.fileName.split(".").last()


    TableRow {
        TableCell {
            Box {
                attrs.sx {
                    display = Display.flex
                    flexDirection = FlexDirection.row
                    alignItems = AlignItems.center
                }
                Avatar {
                    attrs {
                        src = ApiClient.getLocalLink("/${props.file.fileId}/thumbnail")
                        variant = AvatarVariant.square
                        sx {
                            marginRight = 20.px
                        }
                    }
                }
                if (editing) {
                    TextField {
                        attrs {
                            margin = FormControlMargin.none
                            required = true
                            autoFocus = true
                            hiddenLabel = true
                            fullWidth = true
                            name = "Filename"
                            variant = FormControlVariant.standard
                            size = Size.small
                            label = ReactNode("Filename")
                            defaultValue = props.file.fileName.removeSuffix(extension)
                            if (invalidFileName) {
                                helperText = ReactNode("Invalid filename")
                                error = true
                            }

                            onKeyDown = { event ->
                                if (event.key == "Enter") {
                                    val newName = (event.target as HTMLInputElement).value + extension
                                    if (props.file.fileName != newName) {
                                        if (validateFilename(newName)) {
                                            props.renameFile(props.file, newName)
                                            (event.target as HTMLInputElement).blur()
                                        } else {
                                            invalidFileName = true
                                        }
                                    } else {
                                        (event.target as HTMLInputElement).blur()
                                    }
                                }
                            }

                            onBlur = {
                                editing = false
                                invalidFileName = false
                            }
                        }
                    }
                } else {
                    Link {
                        attrs.href = ApiClient.getLocalLink("/${props.file.link}")
                        attrs.target = WindowTarget._blank
                        attrs.underline = LinkUnderline.none
                        +props.file.fileName
                    }
                }
            }
        }
        TableCell {
            +props.file.formattedUploadDate
        }
        TableCell {
            +props.file.humanReadableByteSize()
        }
        TableCell {
            Box {
                attrs.sx {
                    display = Display.flex
                    flexDirection = FlexDirection.row
                    alignItems = AlignItems.center
                }
                IconButton {
                    attrs {
                        size = Size.small
                        color = IconButtonColor.info
                        onClick = {
                            editing = true
                            invalidFileName = false
                        }
                    }

                    Edit {

                    }
                }
                IconButton {
                    attrs {
                        size = Size.small
                        color = IconButtonColor.error
                        onClick = {
                            props.deleteFile(props.file)
                        }
                    }

                    DeleteOutline {

                    }
                }
            }
        }
    }
}

fun RBuilder.file(handler: FileComponentProps.() -> Unit) = child(FileComponent) {
    attrs { handler() }
}