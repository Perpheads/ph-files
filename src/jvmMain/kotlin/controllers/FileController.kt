package com.perpheads.files.controllers

import com.perpheads.files.*
import com.perpheads.files.NotFoundException
import com.perpheads.files.daos.FileDao
import com.perpheads.files.data.File
import com.perpheads.files.data.FileResponse
import com.perpheads.files.data.UploadResponse
import io.ktor.application.*
import io.ktor.features.BadRequestException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.locations.post
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.routing.delete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinInstant
import org.apache.commons.codec.digest.DigestUtils
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt
import java.io.File as JFile

fun Route.fileRoutes(
    fileDao: FileDao,
    config: PhFilesConfig
) {
    val containingFolder = config.filesFolder
    if (containingFolder.exists() && !containingFolder.isDirectory) {
        throw RuntimeException("filesFolder exists but is file")
    } else if (!containingFolder.exists() && !containingFolder.mkdirs()) {
        throw RuntimeException("filesFolder does not exist and failed to create")
    }


    val thumbnailSize = 128

    val secureRandom = SecureRandom()

    fun getFileExtensionFromName(name: String): String {
        return if (name.contains(".")) {
            val suffix = name.substring(name.lastIndexOf(".") + 1)
            return "." + suffix.substring(0, min(4, suffix.length))
        } else ""
    }

    fun getFile(name: String): JFile {
        return JFile(containingFolder.absolutePath + JFile.separator + "$name.dat")
    }

    fun md5File(fileId: Int, file: JFile): String {
        return file.inputStream().use {
            DigestUtils.md5Hex(it).lowercase()
        }
    }

    fun tryGenerateThumbnail(fileId: Int, outputFile: JFile, mimeType: String) {
        if (mimeType != "image/png" && mimeType != "image/jpeg") return
        val image = ImageIO.read(outputFile)
        val thumbImage = BufferedImage(thumbnailSize, thumbnailSize, BufferedImage.TYPE_INT_RGB)
        val g = thumbImage.createGraphics()

        val shorterSide = min(image.height, image.width)
        val scaleFactor = thumbnailSize.toDouble() / shorterSide
        val scaledWidth = (image.width * scaleFactor).roundToInt()
        val scaledHeight = (image.height * scaleFactor).roundToInt()
        val x = -(scaledWidth - thumbnailSize) / 2
        val y = -(scaledHeight - thumbnailSize) / 2
        g.drawImage(image, x, y, scaledWidth, scaledHeight, Color(255, 255, 255, 0), null)

        image.flush()
        val outputArrStream = ByteArrayOutputStream()
        ImageIO.write(thumbImage, "jpg", outputArrStream)
        thumbImage.flush()
        val thumbnail = outputArrStream.toByteArray()

        fileDao.updateThumbnail(fileId, thumbnail)
    }

    suspend fun upload(userId: Int, filePart: PartData.FileItem): File {
        val randomFileName = secureRandom.alphaNumeric(16)
        val link = randomFileName + getFileExtensionFromName(filePart.originalFileName ?: "")
        val mimeType = (filePart.contentType ?: ContentType.Application.OctetStream).toString()
        return withContext(Dispatchers.IO) {
            val fileId = fileDao.create(
                File(
                    fileId = -1,
                    link = link,
                    fileName = filePart.originalFileName ?: randomFileName,
                    mimeType = mimeType,
                    userId = userId,
                    uploadDate = Instant.now(),
                    size = 0,
                    thumbnail = null,
                    md5 = null
                )
            )
            val outputFile = getFile(fileId.toString())
            outputFile.parentFile.mkdir()

            filePart.streamProvider().use { inputStream ->
                Files.copy(inputStream, outputFile.toPath())
            }
            val md5 = md5File(fileId, outputFile)
            val length = outputFile.length()
            fileDao.updateMD5(fileId, md5, length.toInt())

            launch {
                runCatching {
                    tryGenerateThumbnail(fileId, outputFile, mimeType)
                }
            }
            fileDao.findById(fileId) ?: throw RuntimeException("Could not find file that was just uploaded")
        }
    }

    get<FileRoute> {
        val md5Header = call.request.headers["If-None-Match"]?.lowercase()
        val file = withContext(Dispatchers.IO) {
            fileDao.findByLink(it.link)
        } ?: throw NotFoundException("File not found")
        val fileMD5 = file.md5
        call.response.cacheControl(CacheControl.MaxAge(604800))
        if (md5Header?.lowercase() == fileMD5) {
            call.respond(HttpStatusCode.NotModified)
        } else {
            fileMD5?.let {
                call.response.etag(fileMD5)
            }
            val contentType = if (file.mimeType.lowercase().contains("html")) {
                ContentType.Text.Plain
            } else ContentType.parse(file.mimeType)

            val diskFile = getFile(file.fileId.toString())
            val fileContent = LocalFileContent(diskFile, contentType)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, file.fileName)
                    .toString()
            )
            call.respond(fileContent)
        }
    }


    requireUser(AuthorizationType.API_KEY) {
        post<UploadRoute> {
            val resource = call.receiveMultipart()
            val firstPart = resource.readPart()
            if (firstPart == null || firstPart.name != "file" || firstPart !is PartData.FileItem) {
                throw BadRequestException("Invalid file upload request")
            }
            val file = upload(1, firstPart)
            call.respond(UploadResponse(file.link))
        }
    }

    val dateFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())

    requireUser(AuthorizationType.COOKIE) {
        post<UploadCookieRoute> {
            val resource = call.receiveMultipart()
            val firstPart = resource.readPart()
            if (firstPart == null || firstPart.name != "file" || firstPart !is PartData.FileItem) {
                throw BadRequestException("Invalid file upload request")
            }
            val file = upload(1, firstPart)
            call.respond(FileResponse(
                fileId = file.fileId,
                link = file.link,
                fileName = file.fileName,
                mimeType = file.mimeType,
                uploadDate = file.uploadDate.toKotlinInstant(),
                formattedUploadDate = dateFormatter.format(file.uploadDate),
                size = file.size,
                thumbnail = null,
                hasThumbnail = file.thumbnail != null
            ))
        }

        delete<FileRoute> { request ->
            val file = withContext(Dispatchers.IO) {
                fileDao.findByLink(request.link)
            } ?: throw NotFoundException("File not found")
            if (file.userId != call.user().userId) {
                throw ForbiddenException("Not your file!")
            }
            withContext(Dispatchers.IO) {
                fileDao.delete(file.fileId)
                getFile(file.fileId.toString()).delete()
            }
            call.respondText("")
        }
    }
}