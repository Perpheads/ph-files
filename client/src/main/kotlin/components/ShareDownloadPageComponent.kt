package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.ShareFileResponse
import com.perpheads.files.useScope
import csstype.px
import js.core.get
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mui.icons.material.Download
import mui.material.Button
import mui.material.ButtonColor
import mui.material.ButtonVariant
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.*
import react.router.useParams


val ShareDownloadComponent = fc<Props>("ShareDownloadComponent") {
    val param = useParams()
    val token = param["token"] ?: ""

    var fileResponse by useState<ShareFileResponse>()
    var downloading by useState(false)
    var currentJob by useState<Job?>(null)
    val scope = useScope()

    useEffect(param) {
        currentJob?.cancel()
        currentJob = scope.launch {
            downloading = false
            fileResponse = ApiClient.getSharedFileInformation(token)
        }
    }

    Page {
        attrs.name = "Download File"
        attrs.searchBarEnabled = false

        fileResponse?.let { file ->
            Typography {
                attrs.variant = TypographyVariant.h5
                attrs.gutterBottom = true
                +"Downloading file:"
            }

            SharePreviewComponent {
                attrs.file = file
            }

            Button {
                attrs.href = ApiClient.getLocalLink("/share/$token/download")
                attrs.sx {
                    marginTop = 16.px
                }
                attrs.color = ButtonColor.secondary
                attrs.disabled = downloading
                attrs.variant = ButtonVariant.contained
                attrs.endIcon = Download.create()
                attrs.onClick = {
                    downloading = true
                }
                +"Download"
            }
        }
    }
}
