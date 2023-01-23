package com.perpheads.files.components

import com.perpheads.files.wrappers.alpha
import com.perpheads.files.wrappers.styled
import csstype.*
import js.core.jso
import mui.material.InputBase
import mui.system.Breakpoint
import react.dom.html.ReactHTML

val SearchBar = ReactHTML.div.styled { _, theme ->
    position = Position.relative
    borderRadius = theme.shape.borderRadius
    backgroundColor = alpha(theme.palette.common.white, 0.15)
    marginRight = theme.spacing(2)
    marginLeft = 0.px
    width = 100.pct
    "&:hover" {
        backgroundColor = alpha(theme.palette.common.white, 0.25)
    }
    theme.breakpoints.up(Breakpoint.sm).invoke {
        marginLeft = theme.spacing(3)
        width = Length.intrinsic
    }
}

val SearchIconWrapper = ReactHTML.div.styled { _, theme ->
    padding = theme.spacing(0, 2)
    height = 100.pct
    position = Position.absolute
    pointerEvents = "none".asDynamic() as? PointerEvents
    display = Display.flex
    alignItems = AlignItems.center
    justifyContent = JustifyContent.center
}

val StyledInputBase = InputBase.styled {_, theme ->
    color = "inherit".unsafeCast<ColorProperty>()
    "& .MuiInputBase-input" {
        padding = theme.spacing(1, 1, 1, 0)
        paddingLeft = 1.em + theme.spacing(4)
        transition = theme.transitions.create(arrayOf("width"), jso())
        width = 100.pct
        theme.breakpoints.up(Breakpoint.md).invoke {
            width = 20.ch
        }
    }
}