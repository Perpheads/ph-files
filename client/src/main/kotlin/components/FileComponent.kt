package com.perpheads.files.components

import com.perpheads.files.data.FileResponse
import com.perpheads.files.data.validateFilename
import csstype.AlignItems
import csstype.Display
import csstype.FlexDirection
import csstype.px
import mui.icons.material.DeleteOutline
import mui.icons.material.Edit
import mui.icons.material.MoreVert
import mui.material.*
import mui.system.sx
import react.*
import react.dom.onChange
import web.html.HTMLInputElement

external interface FileComponentProps : Props {
    var file: FileResponse
    var deleteFile: (FileResponse) -> Unit
    var renameFile: (FileResponse, String) -> Unit
}

val FileComponent = fc<FileComponentProps>("FileComponent") { props ->
    var editing by useState(false)
    var invalidFileName by useState(false)
    val extension = "." + props.file.fileName.split(".").last()

    val imgSrc = "/${props.file.fileId}/thumbnail"

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
                        src = "https://files.perpheads.com/lME03h0tS1b6dNNj.jpg"
                        variant = AvatarVariant.square
                        sx {
                            marginRight = 12.px
                        }
                    }
                }
                if (editing) {
                    TextField {
                        attrs {
                            margin = FormControlMargin.none
                            required = true
                            fullWidth = true
                            autoFocus = true
                            hiddenLabel = true
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
                    +"Name1"
                }
            }
        }
        TableCell {
            +"Name2"
        }
        TableCell {
            +"Name3"
        }
        TableCell {
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

fun RBuilder.file(handler: FileComponentProps.() -> Unit) = child(FileComponent) {
    attrs { handler() }
}