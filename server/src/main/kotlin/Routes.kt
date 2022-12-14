package com.perpheads.files

import io.ktor.server.locations.*

@Location("/")
object RootRoute

@Location("/account")
data class AccountRoute(val include_thumbnails: Boolean = false)

@Location("/account-info")
object AccountInfoRoute

@Location("/contact")
object ContactRoute
@Location("/auth")
object AuthRoute

@Location("/v2/auth")
object AuthRouteV2

@Location("/users")
object UsersRoute

@Location("/upload")
object UploadRoute

@Location("/upload-cookie")
object UploadCookieRoute

@Location("/{id}/thumbnail")
data class ThumbnailRoute(val id: Int)

@Location("/regenerate-thumbnails")
object RegenerateThumbnailsRoute

@Location("/thumbnails")
object ThumbnailsRoute

@Location("/api-key")
object ApiKeyRoute

@Location("/generate-api-key")
object GenerateApiKeyRoute

@Location("/{link}")
data class FileRoute(val link: String)

@Location("/logout")
object LogoutRoute

@Location("/change-password")
object ChangePasswordRoute

@Location("/statistics")
object StatisticsRoute