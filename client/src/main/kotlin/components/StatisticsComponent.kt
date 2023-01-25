package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.StatisticsResponse
import com.perpheads.files.data.humanReadableByteSize
import com.perpheads.files.logoutIfUnauthorized
import com.perpheads.files.useScope
import csstype.Display
import csstype.FlexDirection
import csstype.pct
import csstype.px
import kotlinx.coroutines.launch
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.Props
import react.fc
import react.router.useNavigate
import react.useEffectOnce
import react.useState

val StatisticsPage = fc<Props> {
    val navigate = useNavigate()
    val (statistics, setStatistics) = useState<StatisticsResponse?>(null)
    val scope = useScope()

    useEffectOnce {
        scope.launch {
            logoutIfUnauthorized(navigate) {
                setStatistics(ApiClient.getStatistics())
            }
        }
    }

    Page {
        attrs.searchBarEnabled = false
        attrs.name = "File Statistics"

        Box {
            attrs.sx {
                display = Display.flex
                flexDirection = FlexDirection.column
                width = 100.pct
            }


            Typography {
                attrs.variant = TypographyVariant.h4
                attrs.gutterBottom = true
                +"Statistics"
            }

            Divider {
                attrs.sx {
                    marginTop = 16.px
                    marginBottom = 16.px
                }
            }

            if (statistics == null) return@Box

            Typography {
                attrs.variant = TypographyVariant.h5
                attrs.gutterBottom = true
                +"Overall Statistics"
            }
            Typography {
                attrs.variant = TypographyVariant.body1
                +"Total file count: ${statistics.totalStatistics.fileCount}"
            }
            Typography {
                attrs.variant = TypographyVariant.body1
                +"Total storage used: ${statistics.totalStatistics.storageUsed.humanReadableByteSize()}"
            }
            Divider {
                attrs.sx {
                    marginTop = 16.px
                    marginBottom = 16.px
                }
            }

            Typography {
                attrs.variant = TypographyVariant.h5
                attrs.gutterBottom = true
                +"User Statistics (Top 100)"
            }

            TableContainer {
                Table {
                    TableHead {
                        TableRow {
                            TableCell {
                                +"Name"
                            }
                            TableCell {
                                +"File Count"
                            }
                            TableCell {
                                +"Storage Used"
                            }
                        }
                    }
                    TableBody {
                        for (userStatistics in statistics.userStatistics) {
                            TableRow {
                                TableCell { +userStatistics.name }
                                TableCell { +userStatistics.fileCount.toString() }
                                TableCell { +userStatistics.storageUsed.humanReadableByteSize() }
                            }
                        }
                    }
                }
            }
        }
    }
}