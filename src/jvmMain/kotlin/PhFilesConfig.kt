package com.perpheads.files

import com.zaxxer.hikari.HikariConfig
import java.io.File

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
) {
    fun toHikariConfig(): HikariConfig {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:mysql://${host}:${port}/${database}"
        hikariConfig.username = user
        hikariConfig.password = password
        hikariConfig.maximumPoolSize = maxPoolSize
        return hikariConfig
    }
}

data class CookieConfig(
    val domain: String,
    val secure: Boolean
)

data class PhFilesConfig(
    val database: DatabaseConfig,
    val filesFolder: File,
    val cookie: CookieConfig
)