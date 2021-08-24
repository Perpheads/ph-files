package com.perpheads.files.components

import com.perpheads.files.data.FileResponse
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RProps
import react.dom.*
import react.fc
import styled.*

external interface FileComponentProps : RProps {
    var file: FileResponse
    var deleteFile: (FileResponse) -> Unit
}

val FileComponent = fc<FileComponentProps>("FileComponent") { props ->
    val imgSrc = "/${props.file.fileId}/thumbnail"
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
    }
}

fun RBuilder.file(handler: FileComponentProps.() -> Unit) = child(FileComponent) {
    attrs { handler() }
}