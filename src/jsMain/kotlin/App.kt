package com.perpheads.files

import com.perpheads.files.components.LoginPageComponent
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.Color
import kotlinx.css.color
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.button
import react.dom.render
import react.functionComponent
import react.useState
import styled.css
import styled.styledDiv

fun main() {
    window.onload = {
        render(document.getElementById("root")) {
            child(LoginPageComponent)
        }
    }
}