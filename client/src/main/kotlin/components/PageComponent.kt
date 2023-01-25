package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.logout
import com.perpheads.files.useAccount
import com.perpheads.files.useScope
import csstype.*
import kotlinx.coroutines.launch
import mui.icons.material.MoreVert
import mui.icons.material.Search
import mui.material.*
import mui.material.Size
import mui.material.styles.Theme
import mui.material.styles.TypographyVariant
import mui.material.styles.useTheme
import mui.system.Breakpoint
import mui.system.sx
import react.*
import react.dom.html.ReactHTML
import react.router.useNavigate
import web.html.HTMLButtonElement
import web.html.HTMLInputElement

data class AlertData(val text: String, val color: AlertColor)

external interface PageProps : PropsWithChildren {
    var name: String
    var searchBarEnabled: Boolean
    var onSearchChanged: ((String) -> Unit)?

    var currentAlert: AlertData?
    var onAlertHidden: (() -> Unit)?
}

external interface SidebarMenuProps : Props {
    var onDialogSelected: (PageDialogs) -> Unit
    var onLogout: () -> Unit
}

private fun RBuilder.menuItem(text: String, onClicked: () -> Unit) {
    MenuItem {
        attrs.onClick = { onClicked() }
        attrs.key = text
        Typography {
            +text
        }
    }
}

private val SidebarMenu = fc<SidebarMenuProps> { props ->
    var anchorEl by useState<HTMLButtonElement?>(null)
    val (user, _) = useAccount(false)
    val navigate = useNavigate()

    Fragment {
        IconButton {
            attrs {
                edge = IconButtonEdge.end
                size = Size.large
                color = IconButtonColor.inherit
                onClick = {
                    anchorEl = it.currentTarget
                }
            }

            MoreVert {

            }
        }
        Menu {
            attrs.onClose = {
                anchorEl = null
            }
            attrs.anchorEl = anchorEl?.let { anchorEl ->
                {
                    anchorEl
                }
            }
            attrs.open = anchorEl != null

            if (user != null) {
                menuItem("Files") {
                    anchorEl = null
                    navigate("/account")
                }

                menuItem("Get API Key") {
                    anchorEl = null
                    props.onDialogSelected(PageDialogs.API_KEY_DIALOG)
                }

                menuItem("Change Password") {
                    anchorEl = null
                    props.onDialogSelected(PageDialogs.CHANGE_PASSWORD_DIALOG)
                }

                menuItem("File Transfer") {
                    anchorEl = null
                    navigate("/share")
                }

                if (user.admin) {
                    menuItem("Create Account") {
                        anchorEl = null
                        props.onDialogSelected(PageDialogs.CREATE_ACCOUNT_DIALOG)
                    }
                    menuItem("Statistics") {
                        anchorEl = null
                        navigate("/statistics")
                    }
                }

                menuItem("Logout") {
                    anchorEl = null
                    props.onLogout()
                }
            }
        }
    }
}

enum class PageDialogs {
    API_KEY_DIALOG,
    CHANGE_PASSWORD_DIALOG,
    CREATE_ACCOUNT_DIALOG
}

val Page = fc<PageProps> { props ->
    val navigate = useNavigate()
    val (user, _) = useAccount(false)
    var currentAlert by useState(props.currentAlert)
    val theme = useTheme<Theme>()
    val smallScreen = useMediaQuery(theme.breakpoints.down(Breakpoint.md))
    var currentDialog by useState<PageDialogs?>(null)
    val scope = useScope()

    useEffect(props.currentAlert) {
        currentAlert = props.currentAlert
    }


    fun doLogout() {
        scope.launch {
            try {
                ApiClient.logout()
            } catch (_: Exception) {

            }
            logout(navigate)
        }
    }


    Box {
        CssBaseline { }
        attrs.sx {
            display = Display.flex
            flexDirection = FlexDirection.column
        }

        AppBar {
            attrs.component = ReactHTML.nav
            Toolbar {
                Container {
                    attrs.maxWidth = "lg"
                    attrs.sx {
                        display = Display.flex
                        alignItems = AlignItems.center
                        flexDirection = FlexDirection.row
                    }

                    Typography {
                        attrs.variant = TypographyVariant.h5
                        attrs.sx {
                            marginLeft = 10.px
                        }
                        +props.name
                    }
                    Box {
                        attrs.sx {
                            flexGrow = number(1.0)
                            theme.breakpoints.only(Breakpoint.xs).invoke {
                                display = "none".asDynamic() as? Display
                            }
                            theme.breakpoints.only(Breakpoint.sm).invoke {
                                display = Display.flex
                            }
                        }
                    }
                    if (props.searchBarEnabled) {
                        SearchBar {
                            SearchIconWrapper {
                                Search { }
                            }
                            StyledInputBase {
                                attrs.placeholder = "Search..."

                                attrs.onChange = { elem ->
                                    props.onSearchChanged?.let {
                                        it.invoke((elem.currentTarget as HTMLInputElement).value)
                                    }
                                }
                            }
                        }
                    }
                    props.onSearchChanged?.let { onSearch ->
                    }

                    if (user != null) {
                        SidebarMenu {
                            attrs.onDialogSelected = {
                                currentDialog = it
                            }

                            attrs.onLogout = {
                                doLogout()
                            }
                        }
                    }
                }
            }
        }
        Toolbar { }

        Container {
            attrs.maxWidth = "lg"
            attrs.sx {
                paddingTop = 32.px
            }
            Paper {
                attrs.sx {
                    display = Display.flex
                    flexDirection = FlexDirection.column
                    alignItems = AlignItems.center
                    padding = 24.px
                }
                attrs.square = true
                props.children()
            }
        }

        Dialog {
            attrs.open = currentDialog != null
            attrs.onClose = { _, _ ->
                currentDialog = null
            }

            if (!smallScreen) {
                attrs.maxWidth = 800.px
            }

            when (currentDialog) {
                PageDialogs.API_KEY_DIALOG -> {
                    ApiKeyComponent {
                        attrs.key = "api_key"
                        attrs.showAlert = { message, color ->
                            currentAlert = AlertData(message, color)
                        }
                    }
                }

                PageDialogs.CHANGE_PASSWORD_DIALOG -> {
                    ChangePasswordComponent { }
                }

                PageDialogs.CREATE_ACCOUNT_DIALOG -> {
                    CreateAccountComponent {
                        attrs.showAlert = { message, color ->
                            currentAlert = AlertData(message, color)
                        }
                        attrs.onDialogClosed = { currentDialog = null }
                    }
                }

                else -> {}
            }
        }
    }

    Snackbar {
        attrs {
            open = currentAlert != null
            autoHideDuration = 6000
            onClose = { _, _ ->
                props.onAlertHidden?.invoke()
                currentAlert = null
            }
        }
        currentAlert?.let { alert ->
            Alert {
                attrs.onClose = {
                    props.onAlertHidden?.invoke()
                    currentAlert = null
                }
                attrs.sx {
                    width = 100.pct
                }
                attrs.severity = alert.color
                +alert.text
            }
        }
    }
}