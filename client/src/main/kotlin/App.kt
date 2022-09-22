package com.perpheads.files

import com.perpheads.files.components.*
import com.perpheads.files.data.AccountInfo
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.js.jso
import react.*
import react.dom.client.createRoot
import react.router.NavigateFunction
import react.router.Route
import react.router.Routes
import react.router.dom.*
import react.router.useNavigate

fun NavigateFunction.replace(route: String) {
    this(route, jso { replace = true })
}

fun logout(navigate: NavigateFunction) {
    window.localStorage.removeItem("loggedIn")
    navigate.replace("/")
}

inline fun logoutIfUnauthorized(navigate: NavigateFunction, block: () -> Unit) {
    try {
        block()
    } catch (e: ApiClient.UnauthorizedException) {
        logout(navigate)
    }
}

class AccountContextData(
    val account: AccountInfo?,
    val setAccount: StateSetter<AccountInfo?>,
    var loadingAccount: Boolean
)

val AccountContext = createContext<AccountContextData>()

fun useAccount(): Pair<AccountInfo?, StateSetter<AccountInfo?>> {
    val contextData = useContext(AccountContext)
    val navigate = useNavigate()

    useEffectOnce {
        if (contextData.account != null || contextData.loadingAccount) return@useEffectOnce
        contextData.loadingAccount = true
        ApiClient.mainScope.launch {
            logoutIfUnauthorized(navigate) {
                contextData.setAccount(ApiClient.getAccountInfo())
                contextData.loadingAccount = false
            }
        }
    }
    return contextData.account to contextData.setAccount
}

val App = VFC {
    val (account, setAccount) = useState<AccountInfo?>(null)

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