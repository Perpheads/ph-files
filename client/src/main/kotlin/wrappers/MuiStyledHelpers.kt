package com.perpheads.files.wrappers

import csstype.PropertiesBuilder
import emotion.styled.StyledComponent
import emotion.styled.StyledOptions
import js.core.Object
import js.core.jso
import mui.material.styles.Theme
import react.ElementType
import react.Props
import react.PropsWithClassName

private var index = 0
private inline val Props.theme: Theme
    get() = asDynamic().theme as Theme

fun <P : PropsWithClassName> ElementType<P>.styled(
    options: StyledOptions?,
    block: PropertiesBuilder.(P, Theme) -> Unit,
): StyledComponent<P> {
    val style = { props: P ->
        val builder: PropertiesBuilder = jso()
        block(builder, props, props.theme)
        builder
    }

    val defaultOptions: StyledOptions = jso {
        target = "ke-${index++}"
    }

    val finalOptions = Object.assign(defaultOptions, options)

    return styled(this, finalOptions)(style)
}

fun <P : PropsWithClassName> ElementType<P>.styled(
    block: PropertiesBuilder.(P, Theme) -> Unit,
): StyledComponent<P> =
    styled(null, block)
