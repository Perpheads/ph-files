package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.ContactResponse
import kotlinx.coroutines.launch
import kotlinx.css.*
import react.Props
import react.dom.a
import react.dom.div
import react.dom.h4
import react.dom.p
import react.fc
import react.useEffectOnce
import react.useState
import styled.css
import styled.styledDiv

val ContactComponent = fc<Props> {
    var contact by useState<ContactResponse?>(null)

    useEffectOnce {
        ApiClient.mainScope.launch {
            contact = ApiClient.getContact()
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


            h4 {
                +"Contact"
            }

            div("divider") { }

            div("section") {
                val email = contact?.email ?: "Loading..."

                p("flow-text") {
                    +"""
                    If you think any of the files hosted on this page violate copyright or should be removed
                    for any other reason, please contact me at 
                    """.trimIndent()
                    a(href = "mailto:$email") {
                        +email
                    }
                    +"."
                }

                p("flow-text") {
                    +"""
                    Please note that this website is currently on an invite only basis. As such, please do
                    not contact me to get an account.
                    """.trimIndent()

                }
            }
        }
    }
}