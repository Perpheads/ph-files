package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.ApiClient.uploadFile
import com.perpheads.files.Parameters
import com.perpheads.files.data.FileResponse
import com.perpheads.files.logoutIfUnauthorized
import com.perpheads.files.parseQueryString
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.id
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.files.File
import react.*
import react.dom.*
import react.router.useLocation
import react.router.useNavigate
import styled.css
import styled.styledDiv

external interface AccountPageProps : Props {
}

private fun <T> List<T>.prepend(elem: T): List<T> {
    val newList = toMutableList()
    newList.add(0, elem)
    return newList
}

val AccountPageComponent = fc<AccountPageProps>("AccountPageComponent") {
    val location = useLocation()
    val parameters = parseQueryString(location.search.drop(1))
    val navigate = useNavigate()
    val page = parameters["page"]?.toIntOrNull() ?: 1
    val search = parameters["search"] ?: ""

    val (username, setUsername) = useState<String?>(null)
    val (paginationData, setPaginationData) = useState(PaginationData(1, 1, 1, 1))
    val (files, setFiles) = useState<List<FileResponse>>(emptyList())
    val (queueFiles, setQueueFiles) = useState(emptyList<UploadQueueEntry>())

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
            setQueueFiles(queueFiles.prepend(progressEntry))
            val response = uploadFile(file) { progress ->
                progressEntry.progress = progress
                setQueueFiles(queueFiles.toList())
            }
            val newList = files.take(8).prepend(response)
            setFiles(newList)
            setQueueFiles(queueFiles.filter { it !== progressEntry })
        }
    }

    suspend fun loadFiles() {
        logoutIfUnauthorized(navigate) {
            val response = ApiClient.loadFiles(query = search, page = page)
            setPaginationData(
                PaginationData(
                    totalPages = response.totalPages,
                    currentPage = response.currentPage,
                    pageStart = response.pageStart,
                    pageEnd = response.pageEnd
                )
            )
            setFiles(response.files)
        }
    }

    suspend fun loadUsername() {
        logoutIfUnauthorized(navigate) {
            setUsername(ApiClient.getAccountInfo().username)
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

    useEffectOnce {
        ApiClient.mainScope.launch {
            loadUsername()
        }
    }

    div {
        navBar {
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
                            setFiles(newFiles)
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
                attrs.onClick = { event ->
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