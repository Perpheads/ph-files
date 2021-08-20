package com.perpheads.files.controllers

import com.perpheads.files.*
import com.perpheads.files.daos.FileDao
import com.perpheads.files.data.File
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.features.BadRequestException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.apache.commons.codec.digest.DigestUtils
import org.imgscalr.Scalr
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
import java.io.File as JFile

fun Route.fileController(
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
        val outputImage = Scalr.resize(image, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, thumbnailSize)
        val outputArrStream = ByteArrayOutputStream()
        ImageIO.write(outputImage, "jpg", outputArrStream)
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

    requireUser(AuthorizationType.COOKIE) {
        post<UploadCookieRoute> {
            val resource = call.receiveMultipart()
            val firstPart = resource.readPart()
            if (firstPart == null || firstPart.name != "file" || firstPart !is PartData.FileItem) {
                throw BadRequestException("Invalid file upload request")
            }
            val file = upload(1, firstPart)
            val formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.systemDefault())
            val formattedDate = formatter.format(file.uploadDate)
        }
    }
}