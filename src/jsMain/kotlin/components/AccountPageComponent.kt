package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.ApiClient.UnauthorizedException
import com.perpheads.files.data.FileListResponse
import com.perpheads.files.data.FileResponse
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.css.*
import react.*
import react.dom.div
import react.dom.param
import react.router.dom.redirect
import react.router.dom.useHistory
import react.router.dom.useLocation
import styled.css
import styled.styledDiv
import styled.styledUl

external interface AccountPageProps : RProps {
}

private fun <T> List<T>.prepend(elem: T): List<T> {
    val newList = toMutableList()
    newList.add(0, elem)
    return newList
}

val AccountPageComponent = fc<AccountPageProps>("AccountPageComponent") { props ->
    val location = useLocation()
    val parameters = parseQueryString(location.search.drop(1))
    val history = useHistory()
    val page = parameters["page"]?.toIntOrNull() ?: 1
    val search = parameters["search"] ?: ""

    val (username, setUsername) = useState<String?>(null)
    val (paginationData, setPaginationData) = useState(PaginationData(1, 1, 1, 1))
    val (files, setFiles) = useState<List<FileResponse>>(emptyList())

    fun changeUrl(newPage: Int, newSearch: String) {
        val params = Parameters.build {
            set("page", newPage.toString())
            if (newSearch.isNotBlank()) {
                set("search", newSearch)
            }
        }.formUrlEncode()
        history.push("/account?${params}")
    }

    suspend fun loadFiles() {
        try {
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
        } catch (e: UnauthorizedException) {
            history.replace("/")
        } catch (e: Exception) {
            //ignored
        }
    }

    suspend fun loadUsername() {
        try {
            setUsername(ApiClient.getAccountInfo().username)
        } catch (e: UnauthorizedException) {
            history.replace("/")
        } catch (e: Exception) {
            //ignored
        }
    }

    suspend fun doDelete(file: FileResponse) {
        try {
            ApiClient.deleteFile(file.link)
        } catch (e: UnauthorizedException) {
            history.replace("/")
        } catch (e: Exception) {
            //ignored
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
                changeUrl(page, it)
            }
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
    }
}

fun RBuilder.accountPage(handler: AccountPageProps.() -> Unit) = child(AccountPageComponent) {
    attrs { handler() }
}