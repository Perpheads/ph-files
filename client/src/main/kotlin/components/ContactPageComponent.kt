package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.data.ContactResponse
import com.perpheads.files.useScope
import csstype.px
import kotlinx.coroutines.launch
import mui.material.Box
import mui.material.Link
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.Props
import react.fc
import react.useEffectOnce
import react.useState

val ContactComponent = fc<Props> {
    var contact by useState<ContactResponse?>(null)
    val email = contact?.email ?: "Loading..."
    val scope = useScope()

    useEffectOnce {
        scope.launch {
            contact = ApiClient.getContact()
        }
    }

    Page {
        attrs.name = "Contact"
        attrs.searchBarEnabled = false

        Box {
            Typography {
                attrs.variant = TypographyVariant.h4
                attrs.gutterBottom = true
                +"Copyright Infringement"
            }
            Typography {
                attrs.variant = TypographyVariant.body1
                +"""
                    If you think any of the files hosted on this page violate copyright or should be removed
                    for any other reason, please contact me at 
                    """.trimIndent()
                Link {
                    attrs.href = "mailto:$email"
                    +email
                }
                +"."
            }

            Typography {
                attrs.variant = TypographyVariant.h4
                attrs.gutterBottom = true
                attrs.sx {
                    marginTop = 16.px
                }
                +"Account Creation"
            }
            Typography {
                attrs.variant = TypographyVariant.body1
                +"""
                    Please note that this website is currently on an invite only basis. As such, please do
                    not contact me to get an account.
                    """.trimIndent()
            }
        }
    }

    /*

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
    }*/
}