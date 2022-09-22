package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.AccountInfo
import com.perpheads.files.logout
import com.perpheads.files.useAccount
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.Props
import react.RBuilder
import react.dom.*
import react.fc
import react.router.dom.Link
import react.router.useNavigate
import react.useEffectOnce
import styled.*

external interface NavBaProps : Props {
    var message: String
    var search: String
    var showSearchBar: Boolean
    var onSearchChanged: (String) -> Unit
    var user: AccountInfo?
}

val NavBarComponent = fc<NavBaProps>("NavBarComponent") { props ->
    val navigate = useNavigate()
    val (account, _) = useAccount()

    fun doLogout() {
        ApiClient.mainScope.launch {
            try {
                ApiClient.logout()
            } catch (e: Exception) {

            }
            logout(navigate)
        }
    }

    useEffectOnce {
        js("M.Dropdown.init(document.querySelectorAll(\".dropdown-trigger\"))")
        Unit
    }

    nav {
        div("nav-wrapper blue darken-4") {
            ul("left") {
                styledSpan {
                    css {
                        classes += "page-title flow-text"
                        paddingLeft = 12.px
                    }
                    +props.message
                }
            }

            if (props.showSearchBar) {
                styledDiv {
                    css {
                        width = 30.pct
                        margin = "auto"
                    }
                    styledDiv {
                        css {
                            classes += "input-field"
                            paddingTop = 9.px
                        }
                        styledInput(InputType.search) {
                            css {
                                height = 44.px
                                minHeight = 44.px
                                maxHeight = 44.px
                            }
                            attrs {
                                id = "search"
                                value = props.search
                                onChangeFunction = { event ->
                                    val newSearch = (event.target as HTMLInputElement).value
                                    props.onSearchChanged(newSearch)
                                }
                            }
                        }
                        label("label-icon") {
                            attrs.htmlFor = "search"
                            i("material-icons") {
                                +"search"
                            }
                        }
                    }
                }
            }

            styledUl {
                css {
                    classes += "right hide-on-med-and-down"
                    position = Position.absolute
                    top = 0.px
                    right = 0.px
                }
                li {
                    a(classes = "dropdown-trigger") {
                        attrs["constrainwidth"] = "false"
                        attrs["data-target"] = "accountMenu"
                        i("material-icons") { +"more_vert" }
                    }
                }
                styledUl {
                    css {
                        classes += "dropdown-content"
                        marginTop = 67.px
                    }
                    attrs.id = "accountMenu"
                    li {
                        Link {
                            attrs.to = "/account"
                            +"Files"
                        }
                    }
                    li {
                        Link {
                            attrs.to = "/api-key"
                            +"Get API Key"
                        }
                    }
                    li {
                        Link {
                            attrs.to = "/change-password"
                            +"Change Password"
                        }
                    }
                    li {
                        Link {
                            attrs.to = "/share"
                            +"Transfer File"
                        }
                    }
                    if (account?.admin == true) {
                        li {
                            Link {
                                attrs.to = "/create-account"
                                +"Create New Account"
                            }
                        }
                    }
                    li {
                        a {
                            attrs.onClick = { doLogout() }
                            +"Logout"
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.navBar(handler: NavBaProps.() -> Unit) = child(NavBarComponent) {
    attrs { handler() }
}