package com.perpheads.files

import com.perpheads.files.components.*
import com.perpheads.files.data.AccountInfoV2
import js.core.jso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mui.material.PaletteMode
import mui.material.styles.ThemeProvider
import mui.material.styles.createTheme
import react.*
import react.dom.client.createRoot
import react.router.*
import react.router.dom.HashRouter
import web.dom.document
import web.window.window

fun NavigateFunction.replace(route: String) {
    this(route, jso { replace = true })
}

fun logout(navigate: NavigateFunction) {
    kotlinx.browser.window.localStorage.removeItem("loggedIn")
    navigate.replace("/")
}

/**
 * Logs out if [navigate] is not null and the user is not authorized.
 */
inline fun catchUnauthorized(navigate: NavigateFunction?, block: () -> Unit) {
    try {
        block()
    } catch (e: ApiClient.UnauthorizedException) {
        navigate?.let { logout(it) }
    }
}

inline fun logoutIfUnauthorized(navigate: NavigateFunction, block: () -> Unit) {
    catchUnauthorized(navigate, block)
}

class AccountContextData(
    val account: AccountInfoV2?,
    val setAccount: StateSetter<AccountInfoV2?>,
    var loadingAccount: Boolean
)

val AccountContext = createContext<AccountContextData>()

fun useScope(): CoroutineScope {
    val scope = MainScope()
    useEffectOnce {
        cleanup {
            scope.cancel()
        }
    }
    return scope
}

fun useAccount(required: Boolean = true): Pair<AccountInfoV2?, StateSetter<AccountInfoV2?>> {
    val contextData = useContext(AccountContext)!!
    val navigate = useNavigate()

    useEffectOnce {
        if (contextData.account != null || contextData.loadingAccount) return@useEffectOnce
        contextData.loadingAccount = true
        val scope = MainScope()

        scope.launch {
            try {
                catchUnauthorized(navigate.takeIf { required }) {
                    contextData.setAccount(ApiClient.getAccountInfo())
                    contextData.loadingAccount = false
                }
            } finally {
                contextData.loadingAccount = false
            }
        }

        cleanup {
            scope.cancel()
            contextData.loadingAccount = false
        }
    }
    return contextData.account to contextData.setAccount
}

private fun ChildrenBuilder.route(builder: (PathRouteProps).() -> Unit) {
    Route {
        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        builder(this as PathRouteProps)
    }
}

val App = VFC {
    val (account, setAccount) = useState<AccountInfoV2?>(null)

    val paletteMode = if (kotlinx.browser.window.matchMedia("(prefers-color-scheme: dark)").matches) {
        PaletteMode.dark
    } else {
        PaletteMode.light
    }

    val usedTheme = createTheme(jso {
        palette = jso {
            mode = paletteMode
        }
    })

    AccountContext.Provider {
        value = AccountContextData(account, setAccount, false)

        ThemeProvider {
            theme = usedTheme
            HashRouter {
                Routes {
                    route {
                        path = "/account"
                        element = createElement(AccountPageComponent)
                    }
                    route {
                        path = "/change-password"
                        element = createElement(ChangePasswordComponent)
                    }
                    route {
                        path = "/api-key"
                        element = createElement(ApiKeyComponent)
                    }
                    route {
                        path = "/share"
                        element = createElement(ShareComponent)
                    }
                    route {
                        path = "/create-account"
                        element = createElement(CreateAccountComponent)
                    }
                    route {
                        path = "/share/:token"
                        element = createElement(ShareDownloadComponent)
                    }
                    route {
                        path = "/contact"
                        element = createElement(ContactComponent)
                    }
                    route {
                        path = "/statistics"
                        element = createElement(StatisticsPage)
                    }
                    route {
                        path = "/"
                        element = createElement(LoginPageComponent)
                    }
                }
            }
        }
    }
}

fun main() {
    window.onload = {
        document.getElementById("root")?.let { rootElem ->
            val reactRoot = createRoot(rootElem)
            reactRoot.render(App.create())
        }
    }
}