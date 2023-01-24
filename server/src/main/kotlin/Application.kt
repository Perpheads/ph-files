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
import io.ktor.server.request.*
import io.ktor.server.websocket.*
import io.ktor.util.date.*
import org.flywaydb.core.Flyway
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Duration


fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)


fun ApplicationCall.getCookieDomain(config: PhFilesConfig): String {
    return if (config.development) {
        request.host()
    } else{
        config.cookie.domain
    }
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val logger = LoggerFactory.getLogger("Application")
    logger.info("Starting application")

    logger.info("Starting Koin")

    install(Koin) {
        modules(PhFilesModule.module)
    }

    logger.info("Starting flyway migration")
    val flyway by inject<Flyway>()
    flyway.migrate()
    logger.info("Flyway migration successful")

    val userDao by inject<UserDao>()
    val cookieDao by inject<CookieDao>()
    val fileDao by inject<FileDao>()

    val phConfig by inject<PhFilesConfig>()

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
        allowCredentials = true
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
                    call.response.cookies.append(
                        name = "id",
                        value = "",
                        expires = GMTDate.START,
                        domain = call.getCookieDomain(phConfig),
                        secure = phConfig.cookie.secure,
                        httpOnly = true,
                        extensions = mapOf("SameSite" to "Strict"),
                        path = "/"
                    )
                    call.respond(message = cause.content ?: "", status = HttpStatusCode.Unauthorized)
                }
                is HttpException -> {
                    call.respond(message = cause.content ?: "", status = cause.statusCode)
                }
                else -> {
                    cause.printStackTrace()
                    call.respond(
                        message = "Oops, something went wrong with your request.\nPlease try again later.",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }

    routing {
        accountRoutes(userDao, cookieDao, phConfig, fileDao)
        fileRoutes(fileDao, phConfig)
        shareRoutes()
        static("/") {
            defaultResource("index.html")
            resource("favicon.ico")
            resource("favicon.png")
            resource("index.html")
            resource("logo.png")
            resource("logo-dark.png")
            resource("thumbnail.png")
            resource("client.js")
            resource("client.js.map")
        }
    }

    logger.info("Application started")
}


abstract class HttpException(val content: Any?, val statusCode: HttpStatusCode) : RuntimeException("")

class ForbiddenException(content: Any? = null) : HttpException(content, HttpStatusCode.Forbidden)
class UnauthorizedException(content: Any? = null) : HttpException(content, HttpStatusCode.Unauthorized)
class BadRequestException(content: Any? = null) : HttpException(content, HttpStatusCode.BadRequest)
class NotFoundException(content: Any? = null) : HttpException(content, HttpStatusCode.NotFound)
class ConflictException(content: Any? = null) : HttpException(content, HttpStatusCode.Conflict)
class NotAcceptableException(content: Any? = null) : HttpException(content, HttpStatusCode.NotAcceptable)
class InternalServerErrorException(content: Any? = null) : HttpException(content, HttpStatusCode.InternalServerError)
