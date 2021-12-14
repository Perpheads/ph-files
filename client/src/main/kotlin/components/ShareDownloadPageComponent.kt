package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.humanReadableByteSize
import data.ShareFileResponse
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import react.Props
import react.dom.div
import react.dom.onClick
import react.fc
import react.router.useParams
import react.useEffectOnce
import react.useState
import styled.css
import styled.styledA
import styled.styledButton
import styled.styledDiv


val ShareDownloadComponent = fc<Props>("ShareDownloadComponent") {
    val param = useParams()
    val token = param["token"] ?: ""

    var fileResponse by useState<ShareFileResponse>()
    var downloading by useState(false)

    useEffectOnce {
        MainScope().launch {
            fileResponse = ApiClient.getSharedFileInformation(token)
        }
    }

    div {
        navBar {
            message = "Download File"
            showSearchBar = false
            onSearchChanged = {}
        }
        div("container") {
            styledDiv {
                css {
                    classes += "card fadeIn animated"
                    paddingBottom = 18.px
                    height = 100.pct
                    paddingTop = 10.px
                    paddingRight = 10.px
                    paddingLeft = 10.px
                }
                div("col l4 center-align") {
                    fileResponse?.let { file ->
                        div {
                            +"Downloading file: ${file.fileName}, Size: ${file.size.humanReadableByteSize()}"
                        }
                        styledA {
                            css {
                                classes += "btn waves-effect waves-light"
                                if (downloading) {
                                    classes += "disabled"
                                }
                            }
                            attrs.href = "/share/$token/download"
                            attrs.onClick = {
                                downloading = true
                            }

                            +"Download"
                        }
                    }
                }
            }
        }
    }
}
