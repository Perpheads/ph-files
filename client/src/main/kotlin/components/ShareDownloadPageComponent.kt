package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.ShareFileResponse
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.js.get
import react.*
import react.dom.div
import react.dom.onClick
import react.dom.p
import react.router.useParams
import styled.css
import styled.styledA
import styled.styledDiv


val ShareDownloadComponent = fc<Props>("ShareDownloadComponent") {
    val param = useParams()
    val token = param["token"] ?: ""

    var fileResponse by useState<ShareFileResponse>()
    var downloading by useState(false)

    useEffect(param) {
        MainScope().launch {
            downloading = false
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
                        p {
                            +"Downloading file:"
                        }
                        SharePreviewComponent {
                            attrs.file = file
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
