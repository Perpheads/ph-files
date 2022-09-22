package com.perpheads.files.components

import com.perpheads.files.data.humanReadableByteSize
import data.ShareFileResponse
import kotlinx.css.*
import react.Props
import react.dom.p
import react.fc
import styled.css
import styled.styledDiv

external interface SharePreviewProps : Props {
    var file: ShareFileResponse
}

val SharePreviewComponent = fc<SharePreviewProps>("SharePreviewComponent") { props ->
    val file = props.file
    styledDiv {
        css {
            classes += "card"
            paddingTop = 4.px
            paddingRight = 4.px
            paddingLeft = 4.px
            paddingBottom = 4.px
        }
        p {
            +"File: ${file.fileName}, Size: ${file.size.humanReadableByteSize()}"
        }
    }
}
