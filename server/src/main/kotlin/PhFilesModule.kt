package com.perpheads.files

import com.perpheads.files.daos.CookieDao
import com.perpheads.files.daos.FileDao
import com.perpheads.files.daos.UserDao
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import io.github.config4k.extract
import org.flywaydb.core.Flyway
import org.jooq.Configuration
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.koin.dsl.module
import javax.sql.DataSource

object PhFilesModule {
    val module = module {
        single<PhFilesConfig> {
            ConfigFactory.load().extract("phFiles")
        }

        single<DataSource> {
            val config = get<PhFilesConfig>().database
            HikariDataSource(config.toHikariConfig())
        }

        single {
            val config = get<PhFilesConfig>().database
            Flyway.configure()
                .dataSource(get())
                .schemas(config.database)
                .load()
        }

        single {
            val config = get<PhFilesConfig>().database
            DefaultConfiguration()
                .set(SQLDialect.MYSQL)
                .set(get<DataSource>())
        }

        single {
            UserDao(get())
        }

        single {
            FileDao(get())
        }

        single {
            CookieDao(get())
        }
    }
}