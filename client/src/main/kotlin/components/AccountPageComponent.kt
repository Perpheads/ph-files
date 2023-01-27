package com.perpheads.files.components

import com.perpheads.files.*
import com.perpheads.files.ApiClient.uploadFile
import com.perpheads.files.data.FileResponse
import csstype.Position
import csstype.WhiteSpace
import csstype.pct
import csstype.px
import js.core.asList
import kotlinx.coroutines.launch
import kotlinx.css.Display
import kotlinx.css.display
import kotlinx.html.InputType
import kotlinx.html.id
import mui.icons.material.Add
import mui.material.*
import mui.material.styles.Theme
import mui.material.styles.TypographyVariant
import mui.material.styles.useTheme
import mui.system.Breakpoint
import mui.system.sx
import react.*
import react.dom.onChange
import react.router.useLocation
import react.router.useNavigate
import styled.css
import styled.styledInput
import web.dom.document
import web.file.File
import web.html.HTMLInputElement

external interface AccountPageProps : Props {
}

private fun <T> List<T>.prepend(elem: T): List<T> {
    val newList = toMutableList()
    newList.add(0, elem)
    return newList
}

private var uploadId: Int = 0


private fun RBuilder.tableHeader(text: String, body: (RElementBuilder<TableCellProps>).() -> Unit = {}) {
    TableCell {
        Typography {
            attrs {
                variant = TypographyVariant.h6
            }
            +text
        }
        body()
    }

}

data class PaginationData(
    val totalPages: Int,
    val currentPage: Int,
    val pageStart: Int,
    val pageEnd: Int
)

val AccountPageComponent = fc<AccountPageProps>("AccountPageComponent") {
    val (account, _) = useAccount()

    val location = useLocation()
    val parameters = parseQueryString(location.search.drop(1))
    val navigate = useNavigate()
    val page = parameters["page"]?.toIntOrNull() ?: 1
    val search = parameters["search"] ?: ""
    val username = account?.username

    val theme = useTheme<Theme>()
    val tinyScreen = useMediaQuery(theme.breakpoints.only(Breakpoint.xs))
    val smallScreen = useMediaQuery(theme.breakpoints.down(Breakpoint.md))
    val shouldShowDetails = useMediaQuery(theme.breakpoints.up(Breakpoint.sm))

    var paginationData by useState(PaginationData(1, 1, 1, 1))
    var files by useState<List<FileResponse>>(emptyList())
    var queueFiles by useState(emptyList<UploadQueueEntry>())
    val scope = useScope()

    fun changeUrl(newPage: Int, newSearch: String) {
        val params = Parameters.build {
            set("page", newPage.toString())
            if (newSearch.isNotBlank()) {
                set("search", newSearch)
            }
        }
        navigate("/account?${params}")
    }

    fun doUploadFiles(uploadFiles: List<File>) {
        val entries = uploadFiles.map { file ->
            val progressEntry = UploadQueueEntry(file.name, 0.0, uploadId++)
            scope.launch {
                val response = uploadFile(file) { progress ->
                    progressEntry.progress = progress
                    queueFiles = queueFiles.toList()
                }
                files = files.take(8).prepend(response)
                queueFiles = queueFiles.filter { it !== progressEntry }
            }
            progressEntry
        }
        queueFiles = entries + queueFiles

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
        scope.launch {
            loadFiles()
        }
    }

    Page {
        attrs {
            if (!smallScreen) {
                name = if (username != null) {
                    "Hey there, $username."
                } else {
                    "Hey there."
                }
            }
            searchBarEnabled = true
            onSearchChanged = {
                changeUrl(page, it)
            }
        }

        TableContainer {
            attrs.sx {
                width = 100.pct
            }

            attrs.onDragOver = {
                it.preventDefault()
            }

            attrs.onDrop = {
                it.preventDefault()
                doUploadFiles(it.dataTransfer.files.asList())
            }

            Table {
                TableHead {
                    TableRow {
                        tableHeader("Name")
                        if (shouldShowDetails) {
                            tableHeader("Date")
                            tableHeader("Size")
                        }
                        tableHeader("") {
                            attrs.align = TableCellAlign.right
                            attrs.sx {
                                width = 1.px
                                whiteSpace = WhiteSpace.nowrap
                            }
                        }
                    }
                }
                TableBody {
                    for (f in files) {
                        file {
                            key = f.fileId.toString()
                            file = f
                            showDetails = shouldShowDetails

                            deleteFile = { file ->
                                scope.launch {
                                    doDelete(file)
                                    val newFiles = files.filter { file.fileId != it.fileId }
                                    files = newFiles
                                }
                            }


                            renameFile = { file, newName ->
                                scope.launch {
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

        Pagination {
            attrs {
                size = if (smallScreen) Size.small else Size.medium
                count = paginationData.totalPages
                defaultPage = paginationData.currentPage
                showFirstButton = !tinyScreen
                showLastButton = !tinyScreen
                this.page = paginationData.currentPage
                onChange = { _, num ->
                    changeUrl(num.toInt(), search)
                }
                sx {
                    marginTop = 16.px
                }
            }
        }

        if (queueFiles.isNotEmpty()) {
            uploadQueue {
                entries = queueFiles
            }
        }

        Fab {
            attrs {
                color = FabColor.secondary
                sx {
                    position = Position.fixed
                    right = 32.px
                    bottom = 32.px
                }
                onClick = {
                    document.getElementById("file-input")?.let { elem ->
                        (elem as HTMLInputElement).click()
                    }
                }
            }
            Add { }
        }

        styledInput(InputType.file) {
            css {
                display = Display.none
            }
            attrs.id = "file-input"
            attrs.onChange = { event ->
                (event.target as HTMLInputElement).files?.let { inputFiles ->
                    doUploadFiles(inputFiles.asList())
                }
            }
        }
    }
}