package com.perpheads.files.components

import com.perpheads.files.data.FileResponse
import com.perpheads.files.data.validateFilename
import com.perpheads.files.showToast
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import styled.css
import styled.styledI
import styled.styledImg

external interface FileComponentProps : Props {
    var file: FileResponse
    var deleteFile: (FileResponse) -> Unit
    var renameFile: (FileResponse, String) -> Unit
}

val FileComponent = fc<FileComponentProps>("FileComponent") { props ->
    val (editing, setEditing) = useState(false)
    val extension = "." + props.file.fileName.split(".").last()

    // Might be bad to do this for every component
    useEffectOnce {
        js("M.Dropdown.init(document.querySelectorAll(\".dropdown-trigger\"))")
        Unit
    }

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
            if (editing) {
                input(type = InputType.text) {
                    attrs.autoFocus = true
                    attrs.defaultValue = props.file.fileName.removeSuffix(extension)
                    attrs.onBlur = {
                        setEditing(false)
                    }
                    attrs.onKeyPress = { event ->
                        if (event.key == "Enter") {
                            val newName = (event.target as HTMLInputElement).value + extension
                            if (props.file.fileName != newName) {
                                if (validateFilename(newName)) {
                                    props.renameFile(props.file, newName)
                                    (event.target as HTMLInputElement).blur()
                                } else {
                                    showToast("Invalid filename")
                                }
                            }
                        }
                    }
                }
            } else {
                a(href = "/${props.file.link}", target = "_blank") {
                    +props.file.fileName
                }
            }
        }
        td { +props.file.formattedUploadDate }
        td { +props.file.humanReadableByteSize() }
        td {
            a(classes = "dropdown-trigger") {
                attrs["data-target"] = "dropdown-${props.file.fileId}"
                styledI {
                    css {
                        classes += "material-icons"
                        cursor = Cursor.pointer
                    }
                    +"more_horiz"
                }
            }

            ul(classes = "dropdown-content") {
                attrs.id = "dropdown-${props.file.fileId}"
                li {
                    a {
                    attrs.onClickFunction = { props.deleteFile(props.file) }
                        +"Delete"
                    }
                }
                li {
                    a {
                        attrs.onClickFunction = { setEditing(true) }
                        +"Rename"
                    }
                }
            }
        }
    }
}

fun RBuilder.file(handler: FileComponentProps.() -> Unit) = child(FileComponent) {
    attrs { handler() }
}