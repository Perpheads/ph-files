package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.StatisticsResponse
import com.perpheads.files.data.humanReadableByteSize
import com.perpheads.files.logoutIfUnauthorized
import kotlinx.coroutines.launch
import kotlinx.css.*
import react.Props
import react.dom.*
import react.fc
import react.router.useNavigate
import react.useEffectOnce
import react.useState
import styled.css
import styled.styledDiv

val StatisticsComponent = fc<Props> {
    val navigate = useNavigate()
    val (statistics, setStatistics) = useState<StatisticsResponse?>(null)

    useEffectOnce {
        ApiClient.mainScope.launch {
            logoutIfUnauthorized(navigate) {
                setStatistics(ApiClient.getStatistics())
            }
        }
    }

    div {
        navBar {
            message = "File Statistics"
            showSearchBar = false
        }
        div("container") {
            styledDiv {
                css {
                    classes += "card fadeIn animated"
                    padding(10.px)
                    paddingBottom = 18.px
                    height = 100.pct
                    minHeight = 100.pct
                }
                if (statistics == null) return@styledDiv
                h4 {
                    +"Statistics"
                }
                div("divider") {}

                div("section") {
                    h5 {
                        +"Total Statistics"
                    }
                    h6 {
                        +"Total file count: ${statistics.totalStatistics.fileCount}"
                    }
                    h6 {
                        +"Total storage used: ${statistics.totalStatistics.storageUsed.humanReadableByteSize()}"
                    }
                }
                div("divider") {}

                div("section") {
                    h5 {
                        +"User Statistics (Top 100)"
                    }
                    table {
                        thead {
                            tr {
                                th { +"Name" }
                                th { +"File Count" }
                                th { +"Storage Used" }
                            }
                        }

                        tbody {
                            for (userStatistics in statistics.userStatistics) {
                                tr {
                                    td { +userStatistics.name }
                                    td { +userStatistics.fileCount.toString() }
                                    td { +userStatistics.storageUsed.humanReadableByteSize() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}