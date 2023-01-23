package com.perpheads.files.components

import com.perpheads.files.data.FileResponse
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
}

val FileComponent = fc<FileComponentProps>("FileComponent") { props ->
    var editing by useState(false)

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
                            marginRight = 8.px
                        }
                    }
                }
                if (editing) {
                    TextField {
                        attrs {
                            margin = FormControlMargin.normal
                            required = true
                            fullWidth = true
                            autoFocus = true
                            name = "username"
                            label = ReactNode("Name")
                            onChange = {
                            }
                            onSubmit = {

                            }
                            onBlur = {
                                editing = false
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

    /*
    tr {
        td {
            styledImg(src = imgSrc) {
                css {
                    minWidth = 48.px
                    maxWidth = 48.px
                }
            }
        }
        td {
            a(href = "/${props.file.link}", target = "_blank") {
                +props.file.fileName
            }
        }
        td { +props.file.formattedUploadDate }
        td { +props.file.humanReadableByteSize() }
        td {
            a {
                styledI {
                    css {
                        classes += "material-icons"
                        cursor = Cursor.pointer
                    }
                    attrs.onClickFunction = { props.deleteFile(props.file) }
                    +"delete"
                }
            }
        }
    }*/
}

fun RBuilder.file(handler: FileComponentProps.() -> Unit) = child(FileComponent) {
    attrs { handler() }
}