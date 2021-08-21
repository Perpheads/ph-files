package com.perpheads.files.controllers

import com.perpheads.files.*
import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.FileDao
import com.perpheads.files.daos.UserDao
import com.perpheads.files.data.*
import com.perpheads.files.data.Cookie
import io.ktor.locations.*
import io.ktor.locations.post
import io.ktor.locations.get
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.date.*
import io.ktor.application.*
import io.ktor.http.*
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
    cookieConfig: CookieConfig,
    fileDao: FileDao
) {
    val secureRandom = SecureRandom()

    post<AuthRoute> {
        val request = call.receive<LoginRequest>()
        val user = withContext(Dispatchers.IO) {
            userDao.getByUsername(request.username)
        }
        if (user == null || !BCrypt.checkpw(request.password, user.password)) {
            call.respond(HttpStatusCode.Unauthorized, LoginResponse(error = "User not found or password incorrect"))
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

    val dateFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())

    requireUser(AuthorizationType.COOKIE) {
        get<ApiKeyRoute> {
            call.respondText(call.user().apiKey)
        }

        post<GenerateApiKeyRoute> {
            val newApiKey = secureRandom.alphaNumeric(32)
            withContext(Dispatchers.IO) {
                userDao.updateApiKey(call.user().userId, newApiKey)
            }
            call.respondText(newApiKey)
        }

        post<LogoutRoute> {
            withContext(Dispatchers.IO) {
                cookieDao.delete(call.authCookie())
            }
            call.respondRedirect(application.locations.href(RootRoute))
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

        get<ThumbnailsRoute> {
            val request = call.receive<ThumbnailRequest>()
            if (request.fileIds.size > 100) {
                call.respondText("Requesting too many fileIds", status = HttpStatusCode.BadRequest)
                return@get
            }
            val thumbnails = withContext(Dispatchers.IO) {
                fileDao.getThumbnails(request.fileIds, call.user().userId)
                    .map { (fileId, thumbnail) -> ThumbnailResponse(fileId, thumbnail) }
            }
            call.respond(thumbnails)
        }

        get<AccountRoute> {
            val includeThumbnails = it.includeThumbnails
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
                    thumbnail = if (includeThumbnails) f.thumbnail else null,
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
    }
}