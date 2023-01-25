package com.perpheads.files.components

import csstype.Position
import csstype.pct
import csstype.px
import mui.material.*
import mui.material.styles.Theme
import mui.material.styles.useTheme
import mui.system.Breakpoint
import mui.system.sx
import react.*


data class UploadQueueEntry(
    val fileName: String,
    var progress: Double,
    val uploadId: Int
)
external interface UploadQueueEntryProps: Props {
    var entry: UploadQueueEntry
}

val UploadQueueEntryComponent = fc<UploadQueueEntryProps>("UploadQueueEntryComponent") { props ->
    ListItemText {
        attrs {
            primary = ReactNode(props.entry.fileName)
        }
        attrs.secondary = LinearProgress.create {
            value = props.entry.progress
            variant = LinearProgressVariant.determinate
        }
    }
}

fun RBuilder.uploadQueueEntry(handler: UploadQueueEntryProps.() -> Unit) = child(UploadQueueEntryComponent) {
    attrs { handler() }
}

external interface UploadQueueProps: Props {
    var entries: List<UploadQueueEntry>
}

val UploadQueueComponent = fc<UploadQueueProps>("UploadQueueComponent") { props ->

    val theme = useTheme<Theme>()
    val smallScreen = useMediaQuery(theme.breakpoints.down(Breakpoint.md))

    Card {
        attrs.sx {
            position = Position.fixed
            bottom = 32.px
            left = 32.px
            if (smallScreen) {
                right = 32.px
            } else {
                width = 100.pct
                maxWidth = 600.px
            }
        }

        CardHeader {
            attrs.title = ReactNode("Upload Queue")
        }
        Divider {
            attrs.variant = DividerVariant.fullWidth
        }
        CardContent {
            List {
                for (f in props.entries) {
                    ListItem {
                        attrs.key = f.uploadId.toString()
                        uploadQueueEntry {
                            entry = f
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.uploadQueue(handler: UploadQueueProps.() -> Unit) = child(UploadQueueComponent) {
    attrs { handler() }
}