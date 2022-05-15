package com.perpheads.files

import com.perpheads.files.controllers.accountRoutes
import com.perpheads.files.controllers.fileRoutes
import com.perpheads.files.controllers.shareRoutes
import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.FileDao
import com.perpheads.files.daos.UserDao
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import io.ktor.server.http.content.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.dataconversion.DataConversion
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.koin.core.context.startKoin
import org.slf4j.event.Level
import java.nio.file.attribute.FileTime
import java.time.Duration
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
        options { _, outgoingContent ->
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
        version { _, outgoingContent ->
            outgoingContent.versions
        }
    }

    install(PartialContent) {

    }

    install(ContentNegotiation) {
        json()
    }
    install(Compression) {
        gzip()
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Cookie)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.ContentLength)
        allowHeader(HttpHeaders.ContentEncoding)
        if (phConfig.cors.anyHost) {
            anyHost()
        } else {
            allowHost(phConfig.cors.host, schemes = listOf("http", "https"))
        }
    }

    install(CallLogging) {
        level = Level.INFO
    }
    install(ForwardedHeaders)
    install(XForwardedHeaders)

    install(Locations)

    install(Authorization) {
        setupDaos(userDao, cookieDao)
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = 1000 * 1000 //1MB
        masking = false
    }

    install(StatusPages) {
        exception { call: ApplicationCall, cause: Exception ->
            when (cause) {
                is UnauthorizedException -> {
                    call.response.cookies.appendExpired(
                        name = "id",
                        path = "/",
                        domain = phConfig.cookie.domain
                    )
                    call.respond(message = cause.content ?: "", status = io.ktor.http.HttpStatusCode.Unauthorized)
                }
                is HttpException -> {
                    call.respond(message = cause.content ?: "", status = cause.statusCode)
                }
                else -> {
                    cause.printStackTrace()
                    call.respond(
                        message = "Oops, something went wrong with your request.\nPlease try again later.",
                        status = io.ktor.http.HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
    println("Starting application")

    routing {
        accountRoutes(userDao, cookieDao, phConfig.cookie, fileDao)
        fileRoutes(fileDao, phConfig)
        shareRoutes()
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


abstract class HttpException(val content: Any?, val statusCode: HttpStatusCode) : RuntimeException("")

class ForbiddenException(content: Any? = null) : HttpException(content, HttpStatusCode.Forbidden)
class UnauthorizedException(content: Any? = null) : HttpException(content, HttpStatusCode.Unauthorized)
class BadRequestException(content: Any? = null) : HttpException(content, HttpStatusCode.BadRequest)
class NotFoundException(content: Any? = null) : HttpException(content, HttpStatusCode.NotFound)
class ConflictException(content: Any? = null) : HttpException(content, HttpStatusCode.Conflict)
class NotAcceptableException(content: Any? = null) : HttpException(content, HttpStatusCode.NotAcceptable)
class InternalServerErrorException(content: Any? = null) : HttpException(content, HttpStatusCode.InternalServerError)
