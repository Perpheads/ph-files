package com.perpheads.files.controllers

import com.perpheads.files.*
import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.FileDao
import com.perpheads.files.daos.UserDao
import com.perpheads.files.data.*
import com.perpheads.files.data.Cookie
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.locations.get
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinInstant
import org.apache.commons.codec.digest.DigestUtils
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

fun Route.accountRoutes(
    userDao: UserDao,
    cookieDao: CookieDao,
    config: PhFilesConfig,
    fileDao: FileDao
) {
    val cookieConfig = config.cookie
    val contact = config.contact
    val secureRandom = SecureRandom()

    suspend fun PipelineContext<Unit, ApplicationCall>.handleAuthentication(isV2: Boolean) {
        val request = call.receive<LoginRequest>()
        val user = withContext(Dispatchers.IO) {
            userDao.getByUsername(request.username)
        }
        if (user == null || !BCrypt.checkpw(request.password, user.password)) {
            call.respond(
                if (isV2) LoginResponseV2(error = "User not found or password incorrect")
                else LoginResponse(error = "User not found or password incorrect")
            )
            return
        }
        val cookieStr = secureRandom.alphaNumeric(32)
        val expiryDate = if (request.remember) {
            Instant.now().plus(Duration.ofDays(30))
        } else Instant.now().plus(Duration.ofDays(1))

        val cookieExpiryDate = if (request.remember) {
            GMTDate(expiryDate.toEpochMilli())
        } else null

        withContext(Dispatchers.IO) {
            cookieDao.create(
                Cookie(
                    cookieId = cookieStr,
                    userId = user.userId,
                    expiry = expiryDate
                )
            )
        }
        call.response.cookies.append(
            name = "id",
            value = cookieStr,
            expires = cookieExpiryDate,
            domain = call.getCookieDomain(config),
            secure = cookieConfig.secure,
            httpOnly = true,
            extensions = mapOf("SameSite" to "Strict"),
            path = "/"
        )

        call.respond(
            if (isV2) LoginResponseV2(accountInfo = AccountInfoV2(user.name, user.email, user.admin))
            else LoginResponse(accountInfo = AccountInfo(user.name, user.email))
        )
    }

    post<AuthRoute> {
        handleAuthentication(false)
    }

    post<AuthRouteV2> {
        handleAuthentication(true)
    }

    get<ContactRoute> {
        call.respond(ContactResponse(contact.email))
    }

    val dateFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())

    requireUser(AuthorizationType.COOKIE) {
        get<ApiKeyRoute> {
            call.respond(ApiKeyResponse(call.user().apiKey))
        }

        post<GenerateApiKeyRoute> {
            val newApiKey = withContext(Dispatchers.IO) {
                userDao.generateApiKey(call.user().userId)
            }
            call.respond(ApiKeyResponse(newApiKey))
        }

        post<LogoutRoute> {
            withContext(Dispatchers.IO) {
                cookieDao.delete(call.authCookie())
            }
            call.response.cookies.appendExpired("id")
            call.respondRedirect(application.locations.href(RootRoute))
        }

        post<UsersRoute> {
            if (!call.user().admin) {
                throw ForbiddenException()
            }
            val request = call.receive<CreateUserRequest>()
            if (!request.isValidRequest()) {
                throw BadRequestException()
            }

            withContext(Dispatchers.IO) {
                userDao.createUser(
                    username = request.username,
                    email = request.email,
                    password = request.password
                )
            }

            call.respondText("")
        }

        post<ChangePasswordRoute> {
            val request = call.receive<ChangePasswordRequest>()
            if (request.newPassword.length < 6) {
                call.respondText("Password needs to have at least 6 characters", status = HttpStatusCode.BadRequest)
                return@post
            } else if (request.newPassword.length > 50) {
                call.respondText("Password cannot be longer than 50 characters", status = HttpStatusCode.BadRequest)
            } else if (!BCrypt.checkpw(request.existingPassword, call.user().password)) {
                call.respondText("Password incorrect", status = HttpStatusCode.BadRequest)
            } else {
                withContext(Dispatchers.IO) {
                    userDao.changePassword(call.user().userId, request.newPassword)
                    cookieDao.deleteAllByUser(call.user().userId)
                }
                call.response.cookies.appendExpired(
                    name = "id",
                    path = "/",
                    domain = call.getCookieDomain(config)
                )
                call.respondText("")
            }
        }

        get<ThumbnailRoute> {
            val md5Header = call.request.headers["If-None_Match"]
            val thumbnail = withContext(Dispatchers.IO) {
                fileDao.findById(it.id)?.thumbnail
            }
            call.response.cacheControl(CacheControl.MaxAge(31536000))
            if (thumbnail == null) {
                call.respondRedirect("/thumbnail.png")
                return@get
            }
            val thumbnailMD5 = DigestUtils.md5Hex(thumbnail)
            if (md5Header?.lowercase()?.contains(md5Header) == true) {
                call.respond(HttpStatusCode.NotModified)
            } else {
                call.response.etag(thumbnailMD5)
                call.respondBytes(ContentType.Image.JPEG) { thumbnail }
            }
        }

        val base64Encoder = Base64.getEncoder()
        post<ThumbnailsRoute> {
            val request = call.receive<ThumbnailRequest>()
            if (request.fileIds.size > 100) {
                call.respondText("Requesting too many fileIds", status = HttpStatusCode.BadRequest)
                return@post
            }
            val thumbnails = withContext(Dispatchers.IO) {
                fileDao.getThumbnails(request.fileIds, call.user().userId)
                    .map { (fileId, thumbnail) -> ThumbnailResponse(fileId, base64Encoder.encodeToString(thumbnail)) }
            }
            call.respond(thumbnails)
        }

        get<AccountInfoRoute> {
            val user = call.user()
            call.respond(AccountInfoV2(user.name, user.email, user.admin))
        }

        post<AccountRoute> {
            val includeThumbnails = it.include_thumbnails
            val request = call.receive<SearchRequest>()
            val page = request.page ?: 1
            val entriesPerPage = request.entriesPerPage.coerceIn(1, 100)
            val query = if (request.query.trim().isEmpty()) null else request.query

            val (fileCount, files) = withContext(Dispatchers.IO) {
                fileDao.findFiles(
                    call.user().userId,
                    request.beforeId,
                    (page - 1) * entriesPerPage,
                    entriesPerPage,
                    query
                )
            }
            val totalPages = max(ceil(fileCount / entriesPerPage.toDouble()), 1.0).toInt()
            val pageStart = max(page - max(4 - (totalPages - page), 2), 1)
            val pageEnd = min(page + max(5 - page, 2), totalPages)

            val fileResponses = files.map { f ->
                val formattedDate = dateFormatter.format(f.uploadDate)
                FileResponse(
                    fileId = f.fileId,
                    link = f.link,
                    fileName = f.fileName,
                    mimeType = f.mimeType,
                    uploadDate = f.uploadDate.toKotlinInstant(),
                    formattedUploadDate = formattedDate,
                    size = f.size,
                    thumbnail = null,
                    f.thumbnail != null
                )
            }
            val response = FileListResponse(
                files = fileResponses,
                totalPages = totalPages,
                currentPage = page,
                pageStart = pageStart,
                pageEnd = pageEnd
            )
            call.respond(response)
        }

        get<StatisticsRoute> {
            if (!call.user().admin) {
                throw ForbiddenException()
            }

            val response = withContext(Dispatchers.IO) {
                val userStatistics = fileDao.getUserStatistics()
                val totalStatistics = fileDao.getTotalStatistics()
                StatisticsResponse(totalStatistics, userStatistics)
            }

            call.respond(response)
        }
    }
}