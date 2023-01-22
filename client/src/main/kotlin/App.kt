package com.perpheads.files

import com.perpheads.files.components.*
import com.perpheads.files.data.AccountInfoV2
import js.core.jso
import kotlinx.coroutines.launch
import react.*
import react.dom.client.createRoot
import react.router.NavigateFunction
import react.router.Route
import react.router.Routes
import react.router.dom.HashRouter
import react.router.useNavigate
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

fun useAccount(required: Boolean = true): Pair<AccountInfoV2?, StateSetter<AccountInfoV2?>> {
    val contextData = useContext(AccountContext)
    val navigate = useNavigate()

    useEffectOnce {
        if (contextData.account != null || contextData.loadingAccount) return@useEffectOnce
        contextData.loadingAccount = true
        ApiClient.mainScope.launch {
            catchUnauthorized(navigate.takeIf { required }) {
                contextData.setAccount(ApiClient.getAccountInfo())
                contextData.loadingAccount = false
            }
        }
    }
    return contextData.account to contextData.setAccount
}

val App = VFC {
    val (account, setAccount) = useState<AccountInfoV2?>(null)

    StrictMode {
        AccountContext.Provider {
            value = AccountContextData(account, setAccount, false)
            HashRouter {
                Routes {
                    Route {
                        path = "/account"
                        element = createElement(AccountPageComponent)
                    }
                    Route {
                        path = "/change-password"
                        element = createElement(ChangePasswordComponent)
                    }
                    Route {
                        path = "/api-key"
                        element = createElement(ApiKeyComponent)
                    }
                    Route {
                        path = "/share"
                        element = createElement(ShareComponent)
                    }
                    Route {
                        path = "/create-account"
                        element = createElement(CreateAccountComponent)
                    }
                    Route {
                        path = "/share/:token"
                        element = createElement(ShareDownloadComponent)
                    }
                    Route {
                        path = "/contact"
                        element = createElement(ContactComponent)
                    }
                    Route {
                        path = "/statistics"
                        element = createElement(StatisticsComponent)
                    }
                    Route {
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