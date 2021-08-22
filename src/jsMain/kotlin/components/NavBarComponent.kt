package com.perpheads.files.components

import com.perpheads.files.ApiClient
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import react.router.dom.redirect
import react.router.dom.routeLink
import react.router.dom.useHistory
import styled.*

external interface NavBarProps : RProps {
    var message: String
    var search: String
    var showSearchBar: Boolean
    var onSearchChanged: (String) -> Unit
}

val NavBarComponent = fc<NavBarProps>("NavBarComponent") { props ->
    val history = useHistory()

    fun logout() {
        ApiClient.mainScope.launch {
            ApiClient.logout()
            history.replace("/")
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
                    li { routeLink("/account") { +"Files" } }
                    li { routeLink("/api-key") { +"Get API Key" } }
                    li { routeLink("/change-password") { +"Change Password" } }
                    li {
                        a {
                            attrs.onClick = { logout() }
                            +"Logout"
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.navBar(handler: NavBarProps.() -> Unit) = child(NavBarComponent) {
    attrs { handler() }
}