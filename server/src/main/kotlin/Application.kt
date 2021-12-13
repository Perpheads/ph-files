package com.perpheads.files

import com.perpheads.files.controllers.accountRoutes
import com.perpheads.files.controllers.fileRoutes
import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.FileDao
import com.perpheads.files.daos.UserDao
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.koin.core.context.startKoin
import org.slf4j.event.Level
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.concurrent.TimeUnit


fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val koin = startKoin {
        modules(PhFilesModule.module)
    }.koin

    val flyway = koin.get<Flyway>()
    flyway.migrate()

    val userDao = koin.get<UserDao>()
    val cookieDao = koin.get<CookieDao>()
    val fileDao = koin.get<FileDao>()

    val phConfig = koin.get<PhFilesConfig>()

    install(AutoHeadResponse)
    install(DataConversion) { }

    val staticCachingOptions = CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 604800))
    install(CachingHeaders) {
        options { outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Application.JavaScript -> staticCachingOptions
                ContentType.Text.JavaScript -> staticCachingOptions
                ContentType.Text.Html -> staticCachingOptions
                ContentType.Image.PNG -> staticCachingOptions
                ContentType.Image.XIcon -> staticCachingOptions
                else -> null
            }
        }
    }

    install(ConditionalHeaders) {
        version {
            if (it is LocalFileContent) {
                listOf(LastModifiedVersion(FileTime.from(it.file.lastModified(), TimeUnit.MILLISECONDS)))
            } else {
                emptyList()
            }
        }
    }

    install(PartialContent) {

    }

    install(ContentNegotiation) {
        json(Json { })
    }
    install(Compression) {
        gzip()
    }

    install(CORS) {
        method(HttpMethod.Get)
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Post)
        method(HttpMethod.Delete)
        header(HttpHeaders.Cookie)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.ContentLength)
        header(HttpHeaders.ContentEncoding)
        allowCredentials = true
        if (phConfig.cors.anyHost) {
            anyHost()
        } else {
            host(phConfig.cors.host, schemes = listOf("http", "https"))
        }
    }

    install(CallLogging) {
        level = Level.INFO
    }
    install(ForwardedHeaderSupport)
    install(XForwardedHeaderSupport)

    install(Locations)

    install(Authorization) {
        setupDaos(userDao, cookieDao)
    }

    install(StatusPages) {
        exception<ForbiddenException> { cause ->
            call.respond(message = cause.content, status = HttpStatusCode.Forbidden)
        }
        exception<UnauthorizedException> { cause ->
            call.response.cookies.appendExpired(
                name ="id",
                path = "/",
                domain = phConfig.cookie.domain
            )
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
    println("Starting application")

    routing {
        accountRoutes(userDao, cookieDao, phConfig.cookie, fileDao)
        fileRoutes(fileDao, phConfig)
        static("/") {
            defaultResource("index.html")
            resource("favicon.ico")
            resource("favicon.png")
            resource("index.html")
            resource("logo.png")
            resource("thumbnail.png")
            resource("client.js")
            resource("client.js.map")
        }
    }

    println("Application started")
}


data class ForbiddenException(val content: Any) : RuntimeException("")
data class UnauthorizedException(val content: Any) : RuntimeException("")
data class BadRequestException(val content: Any) : RuntimeException("")
data class NotFoundException(val content: Any) : RuntimeException("")
data class ConflictException(val content: Any) : RuntimeException("")
data class NotAcceptableException(val content: Any) : RuntimeException("")
data class InternalServerErrorException(val content: Any) : RuntimeException("")
data class TooManyRequestsException(val content: Any, val retryAfter: Instant? = null) : RuntimeException("")