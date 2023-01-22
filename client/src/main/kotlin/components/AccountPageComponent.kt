package com.perpheads.files.components

import com.perpheads.files.*
import com.perpheads.files.ApiClient.uploadFile
import com.perpheads.files.data.FileResponse
import js.core.asList
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.id
import react.Props
import react.dom.*
import react.fc
import react.router.useLocation
import react.router.useNavigate
import react.useEffect
import react.useState
import styled.css
import styled.styledDiv
import web.file.File
import web.html.HTMLInputElement

external interface AccountPageProps : Props {
}

private fun <T> List<T>.prepend(elem: T): List<T> {
    val newList = toMutableList()
    newList.add(0, elem)
    return newList
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

    useEffect(location) {
        ApiClient.mainScope.launch {
            loadFiles()
        }
    }

    div {
        navBar {
            val username = account?.username
            message = if (username != null) {
                "Hey there, $username."
            } else {
                "Hey there."
            }
            this.search = search
            showSearchBar = true
            onSearchChanged = {
                changeUrl(1, it)
            }
        }
        div("container") {
            attrs {
                onDragOver = { it.preventDefault() }
                onDrop = { event ->
                    event.preventDefault()
                    event.dataTransfer.files.asList().forEach { file ->
                        doUploadFile(file)
                    }
                }
            }
            styledDiv {
                css {
                    classes += "card fadeIn animated"
                    paddingBottom = 18.px
                    height = 100.pct
                    paddingTop = 10.px
                    paddingRight = 10.px
                    paddingLeft = 10.px
                }
                fileList {
                    this.files = files
                    deleteFile = { file ->
                        ApiClient.mainScope.launch {
                            doDelete(file)
                            val newFiles = files.filter { file.fileId != it.fileId }
                            files = newFiles
                        }
                    }
                }
                paginationComponent {
                    this.paginationData = paginationData
                    this.onPageChange = {
                        changeUrl(it, search)
                    }
                }
            }
        }
        if (queueFiles.isNotEmpty()) {
            styledDiv {
                css {
                    position = Position.absolute
                    bottom = 16.px
                    left = 24.px
                    minWidth = 30.pct
                }
                uploadQueue {
                    entries = queueFiles
                }
            }
        }
        div("fixed-action-btn") {
            a(classes = "btn-floating btn-large red") {
                attrs.onClick = { _ ->
                    document.getElementById("file-input")?.let { elem ->
                        (elem as HTMLInputElement).click()
                    }
                }
                i("large material-icons") {
                    +"add"
                }
                input(InputType.file) {
                    attrs.id = "file-input"
                    attrs.onChange = { event ->
                        (event.target as HTMLInputElement).files?.let { inputFiles ->
                            inputFiles.asList().forEach { file ->
                                doUploadFile(file)
                            }
                        }
                    }
                }
            }
        }
    }
}