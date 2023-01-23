package com.perpheads.files.components

import csstype.*
import js.core.jso
import kotlinx.browser.window
import mui.icons.material.MoreVert
import mui.icons.material.Search
import mui.material.*
import mui.material.Size
import mui.material.styles.TypographyVariant
import mui.material.styles.createTheme
import mui.system.Breakpoint
import mui.system.ThemeProvider
import mui.system.sx
import react.*
import react.dom.html.ReactHTML
import web.html.HTMLButtonElement
import web.html.HTMLInputElement

external interface PageProps : PropsWithChildren {
    var name: String
    var searchBarEnabled: Boolean
    var onSearchChanged: ((String) -> Unit)?
}

private val SidebarMenu = fc<Props> {
    var anchorEl by useState<HTMLButtonElement?>(null)

    Fragment {
        IconButton {
            attrs {
                edge = IconButtonEdge.end
                size = Size.large
                color = IconButtonColor.inherit
                onClick = {
                    anchorEl = it.currentTarget
                }
                sx {
                    marginRight = 10.px
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

            MenuItem {
                attrs.onClick = { anchorEl = null }
                attrs.key = "Files"
                Typography {
                    +"Files"
                }
            }
        }
    }
}

val Page = fc<PageProps> { props ->
    val paletteMode = if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
        PaletteMode.dark
    } else {
        PaletteMode.light
    }

    val theme = createTheme(jso {
        palette = jso {
            mode = paletteMode
        }
    })

    ThemeProvider {
        attrs.theme = theme

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

                        SidebarMenu { }
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
        }

    }
}