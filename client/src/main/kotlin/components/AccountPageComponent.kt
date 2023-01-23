package com.perpheads.files.components

import com.perpheads.files.*
import com.perpheads.files.ApiClient.uploadFile
import com.perpheads.files.data.FileResponse
import csstype.pct
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.*
import react.router.useLocation
import react.router.useNavigate
import web.file.File

external interface AccountPageProps : Props {
}

private fun <T> List<T>.prepend(elem: T): List<T> {
    val newList = toMutableList()
    newList.add(0, elem)
    return newList
}

private fun RBuilder.tableHeader(text: String) {
    TableCell {
        Typography {
            attrs {
                variant = TypographyVariant.h6
            }
            +text
        }
    }

}

val AccountPageComponent = fc<AccountPageProps>("AccountPageComponent") {
    val (account, _) = useAccount()

    val location = useLocation()
    val parameters = parseQueryString(location.search.drop(1))
    val navigate = useNavigate()
    val page = parameters["page"]?.toIntOrNull() ?: 1
    val search = parameters["search"] ?: ""

    var paginationData by useState(PaginationData(1, 1, 1, 1))
    var files by useState<List<FileResponse>>(emptyList())
    var queueFiles by useState(emptyList<UploadQueueEntry>())

    fun changeUrl(newPage: Int, newSearch: String) {
        val params = Parameters.build {
            set("page", newPage.toString())
            if (newSearch.isNotBlank()) {
                set("search", newSearch)
            }
        }
        navigate("/account?${params}")
    }

    fun doUploadFile(file: File) {
        ApiClient.mainScope.launch {
            val progressEntry = UploadQueueEntry(file.name, 0.0)
            queueFiles = queueFiles.prepend(progressEntry)
            val response = uploadFile(file) { progress ->
                progressEntry.progress = progress
                queueFiles = queueFiles.toList()
            }
            files = files.take(8).prepend(response)
            queueFiles = queueFiles.filter { it !== progressEntry }
        }
    }

    suspend fun loadFiles() {
        logoutIfUnauthorized(navigate) {
            val response = ApiClient.loadFiles(query = search, page = page)
            paginationData = PaginationData(
                totalPages = response.totalPages,
                currentPage = response.currentPage,
                pageStart = response.pageStart,
                pageEnd = response.pageEnd
            )

            files = response.files
        }
    }

    suspend fun doDelete(file: FileResponse) {
        logoutIfUnauthorized(navigate) {
            ApiClient.deleteFile(file.link)
        }
    }

    suspend fun doRename(file: FileResponse, newName: String) {
        logoutIfUnauthorized(navigate) {
            ApiClient.renameFile(file.link, newName)
        }
    }

    useEffect(location) {
        ApiClient.mainScope.launch {
            loadFiles()
        }
    }

    Page {
        attrs {
            name = "Hello there!"
            onSearch = {

            }
        }

        TableContainer {
            attrs.sx {
                width = 100.pct
            }
            Table {
                TableHead {
                    TableRow {
                        tableHeader("Name")
                        tableHeader("Date")
                        tableHeader("Size")
                        tableHeader("")
                    }
                }
                TableBody {
                    file {
                        file = FileResponse(
                            fileId = 1,
                            link = "ABHDGKGAW",
                            fileName = "test.jpg",
                            mimeType = "image/jpeg",
                            uploadDate = Clock.System.now(),
                            formattedUploadDate = "22.02.2022 13:00",
                            size = 12315623,
                            thumbnail = "test.png",
                            hasThumbnail = false
                        )

                        deleteFile = { file ->

                        }


                        renameFile = { file, newName ->
                            ApiClient.mainScope.launch {
                                doRename(file, newName)
                                val newFiles = files.map {
                                    if (file.fileId == it.fileId) it.copy(fileName = newName) else it
                                }
                                files = newFiles
                            }
                        }
                    }
                }
            }
        }
    }
}