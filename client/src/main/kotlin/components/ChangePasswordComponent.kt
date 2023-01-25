package com.perpheads.files.components

import com.perpheads.files.ApiClient
import com.perpheads.files.logout
import com.perpheads.files.useScope
import csstype.px
import kotlinx.coroutines.launch
import mui.material.*
import mui.system.sx
import react.Props
import react.ReactNode
import react.dom.html.ButtonType
import react.dom.html.ReactHTML
import react.dom.onChange
import react.fc
import react.router.useNavigate
import react.useState

val ChangePasswordComponent = fc<Props>("ChangePasswordComponent") {
    val navigate = useNavigate()
    val (password, setPassword) = useState("")
    val (newPassword, setNewPassword) = useState("")
    val (repeatNewPassword, setRepeatNewPassword) = useState("")
    val (error, setError) = useState("")
    val scope = useScope()

    fun changePassword() {
        if (password.isEmpty()) {
            setError("Existing password is empty")
        } else if (newPassword.length < 6) {
            setError("New password must be at least 6 characters")
        } else if (repeatNewPassword != newPassword) {
            setError("New password does not match repeated password")
        } else {
            scope.launch {
                try {
                    ApiClient.changePassword(password, newPassword)
                    logout(navigate)
                } catch (e: Exception) {
                    setError("Failed to change password (incorrect existing password?)")
                }
            }
        }
    }

    DialogTitle {
        +"Change Password"
    }
    Divider { }
    DialogContent {
        Box {
            attrs.component = ReactHTML.form
            attrs.sx {
                marginTop = 2.px
            }
            attrs.onSubmit = {
                it.preventDefault()
                changePassword()
            }

            TextField {
                attrs {
                    margin = FormControlMargin.normal
                    required = true
                    fullWidth = true
                    name = "existing password"
                    type = web.html.InputType.password
                    label = ReactNode("Existing Password")
                    onChange = {
                        setPassword((it.target as web.html.HTMLInputElement).value)
                    }
                }
            }

            TextField {
                attrs {
                    margin = FormControlMargin.normal
                    required = true
                    fullWidth = true
                    type = web.html.InputType.password
                    name = "new password"
                    label = ReactNode("New Password")
                    onChange = {
                        setNewPassword((it.target as web.html.HTMLInputElement).value)
                    }
                }
            }

            TextField {
                attrs {
                    margin = FormControlMargin.normal
                    required = true
                    fullWidth = true
                    type = web.html.InputType.password
                    name = "repeat password"
                    label = ReactNode("Repeat New Password")
                    onChange = {
                        setRepeatNewPassword((it.target as web.html.HTMLInputElement).value)
                    }
                }
            }

            FormHelperText {
                attrs.error = error != ""
                +error
            }

            Button {
                attrs {
                    type = ButtonType.submit
                    fullWidth = true
                    variant = ButtonVariant.contained
                    sx {
                        marginTop = 12.px
                        marginBottom = 2.px
                    }
                }
                +"Change Password"
            }
        }
    }
}
