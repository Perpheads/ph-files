package com.perpheads.files

import com.perpheads.files.controllers.accountRoutes
import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.FileDao
import com.perpheads.files.daos.UserDao
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.datetime.Instant
import org.koin.core.context.startKoin


fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val koin = startKoin {
        modules(PhFilesModule.module)
    }.koin

    val userDao = koin.get<UserDao>()
    val cookieDao = koin.get<CookieDao>()
    val fileDao = koin.get<FileDao>()

    val phConfig = koin.get<PhFilesConfig>()

    install(DataConversion) {
        convert<Instant> {
            decode { values, _ ->
                val first = values.firstOrNull() ?: return@decode null
                Instant.parse(first)
            }
            encode { value ->
                when (value) {
                    null -> listOf()
                    is Instant -> listOf(value.toString())
                    else -> throw DataConversionException("Cannot convert $value as Instant")
                }
            }
        }
    }

    install(StatusPages) {
        exception<ForbiddenException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.Forbidden)
        }
        exception<UnauthorizedException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.Unauthorized)
        }
        exception<BadRequestException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.BadRequest)
        }
        exception<NotFoundException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.NotFound)
        }
        exception<ConflictException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.Conflict)
        }
        exception<NotAcceptableException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.NotAcceptable)
        }
        exception<InternalServerErrorException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.InternalServerError)
        }
        exception<Exception> { cause ->
            cause.printStackTrace()
            call.respond(
                message = "Oops, something went wrong with your request.\nPlease try again later.",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    routing {
        accountRoutes(userDao, cookieDao, phConfig.cookie)
    }
}


data class ForbiddenException(val content: Any) : RuntimeException("")
data class UnauthorizedException(val content: Any) : RuntimeException("")
data class BadRequestException(val content: Any) : RuntimeException("")
data class NotFoundException(val content: Any) : RuntimeException("")
data class ConflictException(val content: Any) : RuntimeException("")
data class NotAcceptableException(val content: Any) : RuntimeException("")
data class InternalServerErrorException(val content: Any) : RuntimeException("")
data class TooManyRequestsException(val content: Any, val retryAfter: Instant? = null) : RuntimeException("")