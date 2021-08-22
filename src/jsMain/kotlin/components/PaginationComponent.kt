package com.perpheads.files.components

import react.RBuilder
import react.RProps
import react.dom.*
import react.fc


external interface PaginationArrowComponentProps : RProps {
    var enabled: Boolean
    var left: Boolean
    var destinationPage: Int
    var onPageChange: (Int) -> Unit
}

val PaginationArrowComponent = fc<PaginationArrowComponentProps>("PaginationArrowComponent") { props ->
    val icon = if (props.left) "chevron_left" else "chevron_right"
    if (!props.enabled) {
        li("disabled") {
            i("material-icons") { +icon }
        }
    } else {
        li("waves-effect") {
            a() {
                i("material-icons") { +icon }
                attrs.onClick = { props.onPageChange(props.destinationPage) }
            }
        }
    }
}

fun RBuilder.paginationArrow(handler: PaginationArrowComponentProps.() -> Unit) = child(PaginationArrowComponent) {
    attrs { handler() }
}

data class PaginationData(
    val totalPages: Int,
    val currentPage: Int,
    val pageStart: Int,
    val pageEnd: Int
)

external interface PaginationComponentProps : RProps {
    var paginationData: PaginationData
    var onPageChange: (Int) -> Unit
}

val PaginationComponent = fc<PaginationComponentProps>("PaginationComponent") { props ->
    ul("pagination center-align") {
        //Left arrow
        paginationArrow {
            enabled = props.paginationData.currentPage != 1
            destinationPage = props.paginationData.currentPage - 1
            left = true
            onPageChange = { props.onPageChange(it) }
        }
        //Numbers
        for (page in props.paginationData.pageStart..props.paginationData.pageEnd) {
            val liClass = if (page == props.paginationData.currentPage) "active" else "waves-effect"
            li(liClass) {
                a { +(page.toString()) }
                if (page != props.paginationData.currentPage) {
                    attrs.onClick = { props.onPageChange(page) }
                }
            }
        }

        //Right arrow
        paginationArrow {
            enabled = props.paginationData.currentPage < props.paginationData.pageEnd
            destinationPage = props.paginationData.currentPage + 1
            left = false
            onPageChange = { props.onPageChange(it) }
        }
    }
}

fun RBuilder.paginationComponent(handler: PaginationComponentProps.() -> Unit) = child(PaginationComponent) {
    attrs { handler() }
}