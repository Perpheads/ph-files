package com.perpheads.files.components

import kotlinx.css.*
import react.RBuilder
import react.RProps
import react.dom.*
import react.fc
import styled.css
import styled.styledDiv


data class UploadQueueEntry(
    val fileName: String,
    var progress: Double
)
external interface UploadQueueEntryProps: RProps {
    var entry: UploadQueueEntry
}

val UploadQueueEntryComponent = fc<UploadQueueEntryProps>("UploadQueueEntryComponent") { props ->
    li("collection-item") {
        +props.entry.fileName
        div("progress") {
            styledDiv {
                css {
                    classes += "determinate"
                    width = (props.entry.progress * 100).toInt().pct
                }
            }
        }
    }
}

fun RBuilder.uploadQueueEntry(handler: UploadQueueEntryProps.() -> Unit) = child(UploadQueueEntryComponent) {
    attrs { handler() }
}

external interface UploadQueueProps: RProps {
    var entries: List<UploadQueueEntry>
}

val UploadQueueComponent = fc<UploadQueueProps>("UploadQueueComponent") { props ->
    ul("collection with-header") {
        li("collection-header") {
            h6 { +"Upload Queue" }
        }
        props.entries.forEach { uploadQueueEntry { entry = it } }
    }
}

fun RBuilder.uploadQueue(handler: UploadQueueProps.() -> Unit) = child(UploadQueueComponent) {
    attrs { handler() }
}