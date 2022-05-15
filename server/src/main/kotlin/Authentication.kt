package com.perpheads.files

import com.perpheads.files.Authorization.Feature.apiKeyAttributeKey
import com.perpheads.files.Authorization.Feature.cookieAttributeKey
import com.perpheads.files.Authorization.Feature.userAttributeKey
import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.UserDao
import com.perpheads.files.data.User
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class Authorization(
    private val userDao: UserDao,
    private val cookieDao: CookieDao
) {
    class Configuration() {
        internal var userDao: UserDao? = null
        internal var cookieDao: CookieDao? = null

        fun setupDaos(userDao: UserDao, cookieDao: CookieDao) {
            this.userDao = userDao
            this.cookieDao = cookieDao
        }
    }

    private val challengePhase = PipelinePhase("Challenge")

    fun interceptPipeline(
        pipeline: ApplicationCallPipeline, type: AuthorizationType
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, challengePhase)
        pipeline.insertPhaseAfter(challengePhase, AuthorizationPhase)
        pipeline.intercept(AuthorizationPhase) {
            val user: User = when (type) {
                AuthorizationType.API_KEY -> {
                    val apiKey = call.request.header("API-KEY") ?: throw UnauthorizedException("No API key provided")
                    call.attributes.put(apiKeyAttributeKey, apiKey)
                    withContext(Dispatchers.IO) {
                        userDao.getByApiKey(apiKey) ?: throw UnauthorizedException("API Key does not belong to a user")
                    }
                }
                AuthorizationType.COOKIE -> {
                    val cookie = call.request.cookies["id"] ?: throw UnauthorizedException("Not logged in")
                    call.attributes.put(cookieAttributeKey, cookie)
                    withContext(Dispatchers.IO) {
                        cookieDao.getUserByCookieId(cookie)
                            ?: throw UnauthorizedException("API Key does not belong to a user")
                    }
                }
            }
            call.attributes.put(userAttributeKey, user)
        }
    }


    companion object Feature : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, Authorization> {
        override val key: AttributeKey<Authorization> = AttributeKey("Authorization")
        val AuthorizationPhase = PipelinePhase("Authorization")
        val userAttributeKey: AttributeKey<User> = AttributeKey("user")
        val apiKeyAttributeKey: AttributeKey<String> = AttributeKey("authApiKey")
        val cookieAttributeKey: AttributeKey<String> = AttributeKey("authCookie")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Authorization {
            val configuration = Configuration().apply(configure)
            val userDao = configuration.userDao
            val cookieDao = configuration.cookieDao
            if (userDao == null || cookieDao == null) {
                throw RuntimeException("Need to specify both cookie and user dao")
            }
            return Authorization(userDao, cookieDao)
        }

    }
}

enum class AuthorizationType {
    API_KEY,
    COOKIE
}

fun ApplicationCall.user(): User {
    return attributes.getOrNull(userAttributeKey) ?: throw UnauthorizedException("No user found")
}

fun ApplicationCall.authApiKey(): String {
    return attributes.getOrNull(apiKeyAttributeKey) ?: throw UnauthorizedException("No user found")
}

fun ApplicationCall.authCookie(): String {
    return attributes.getOrNull(cookieAttributeKey) ?: throw UnauthorizedException("No user found")
}


class AuthorizedUserRouteSelector() : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        RouteSelectorEvaluation.Constant

    override fun toString() = "(AuthorizedRoute)"
}

fun Route.requireUser(authorizationType: AuthorizationType, body: Route.() -> Unit): Route {
    val authorizedRoute = createChild(AuthorizedUserRouteSelector())
    application.plugin(Authorization).interceptPipeline(authorizedRoute, authorizationType)
    authorizedRoute.body()
    return authorizedRoute
}
