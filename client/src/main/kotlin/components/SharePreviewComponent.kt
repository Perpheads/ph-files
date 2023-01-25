package com.perpheads.files.components

import com.perpheads.files.data.ShareFileResponse
import com.perpheads.files.data.humanReadableByteSize
import csstype.px
import mui.material.Card
import mui.material.CardContent
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.Props
import react.fc

external interface SharePreviewProps : Props {
    var file: ShareFileResponse
}

val SharePreviewComponent = fc<SharePreviewProps>("SharePreviewComponent") { props ->
    val file = props.file

    Card {
        attrs.sx {
            marginTop = 16.px
            marginBottom = 16.px
            minWidth = 200.px
        }
        CardContent {
            Typography {
                attrs.variant = TypographyVariant.h5
                attrs.gutterBottom = true
                +"File: ${file.fileName}"
            }
            Typography {
                attrs.variant = TypographyVariant.body1
                +"Size: ${file.size.humanReadableByteSize()}"
            }
        }
    }
}
