package com.perpheads.files.controllers

import com.perpheads.files.*
import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.UserDao
import com.perpheads.files.data.Cookie
import data.AccountInfo
import data.ChangePasswordRequest
import data.LoginRequest
import data.LoginResponse
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.date.*
import io.ktor.application.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

fun Route.accountRoutes(
    userDao: UserDao,
    cookieDao: CookieDao,
    cookieConfig: CookieConfig
) {
    val secureRandom = SecureRandom()

    post<AuthRoute> {
        val request = call.receive<LoginRequest>()
        val user = withContext(Dispatchers.IO) {
            userDao.getByUsername(request.username)
        }
        if (user == null || !BCrypt.checkpw(request.password, user.password)) {
            call.respond(LoginResponse(error = "User not found or password incorrect"))
            return@post
        }
        val cookieStr = secureRandom.alphaNumeric(32)
        withContext(Dispatchers.IO) {
            cookieDao.create(
                Cookie(
                    cookieId = cookieStr,
                    userId = user.userId,
                    cookie = "",
                    createDate = Instant.now()
                )
            )
        }
        call.response.cookies.append(
            name = "id",
            value = cookieStr,
            expires = GMTDate(Instant.now().plus(Duration.ofDays(29)).toEpochMilli()),
            domain = cookieConfig.domain,
            secure = cookieConfig.secure,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Strict")
        )

        call.respond(LoginResponse(accountInfo = AccountInfo(user.name, user.email)))
    }

    requireUser(AuthorizationType.COOKIE) {
        get<ApiKeyRoute> {
            withContext(Dispatchers.IO) {
                call.respondText(call.user().apiKey)
            }
        }

        post<GenerateApiKeyRoute> {
            val newApiKey = secureRandom.alphaNumeric(32)
            withContext(Dispatchers.IO) {
                userDao.updateApiKey(call.user().userId, newApiKey)
                call.respondText(newApiKey)
            }
        }

        post<LogoutRoute> {
            withContext(Dispatchers.IO) {
                cookieDao.delete(call.authCookie())
                call.respondRedirect(application.locations.href(RootRoute))
            }
        }

        post<ChangePasswordRoute> {
            val request = call.receive<ChangePasswordRequest>()
            if (request.newPassword.length < 6) {
                call.respondText("Password needs to have at least 6 characters", status = HttpStatusCode.BadRequest)
                return@post
            } else if (request.newPassword.length > 50) {
                call.respondText("Password cannot be longer than 50 characters", status = HttpStatusCode.BadRequest)
            } else if (BCrypt.checkpw(request.existingPassword, call.user().password)) {
                call.respondText("Password incorrect", status = HttpStatusCode.BadRequest)
            } else {
                withContext(Dispatchers.IO) {
                    userDao.changePassword(call.user().userId, request.newPassword)
                    cookieDao.deleteAllByUser(call.user().userId)
                    call.respondText("")
                }
            }
        }
    }

}