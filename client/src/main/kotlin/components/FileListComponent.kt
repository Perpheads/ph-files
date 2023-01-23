package com.perpheads.files.components

import com.perpheads.files.data.FileResponse
import react.Props
import react.RBuilder
import react.dom.*
import react.fc

external interface FileListComponentProps : Props {
    var files: List<FileResponse>
    var deleteFile: (FileResponse) -> Unit
    var renameFile: (FileResponse, String) -> Unit
}

val FileListComponent = fc<FileListComponentProps>("FileListComponent") { props ->
    table("bordered highlight") {
        thead {
            tr {
                th { +"Thumbnail" }
                th { +"Name" }
                th { +"Date" }
                th { +"Size" }
                th { +"" }
            }
        }
        tbody {
            for (f in props.files) {
                file {
                    file = f
                    deleteFile = props.deleteFile
                    renameFile = props.renameFile
                }
            }
        }
    }
}

fun RBuilder.fileList(handler: FileListComponentProps.() -> Unit) = child(FileListComponent) {
    attrs { handler() }
}
