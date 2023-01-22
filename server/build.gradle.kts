buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.flywaydb", "flyway-mysql", "9.3.1")
    }
}

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("nu.studer.jooq") version "7.1.1"
    id("org.flywaydb.flyway") version "9.3.1"
    id("com.bmuschko.docker-java-application") version "8.1.0"
    application
}

val ktor_version: String by project
val logback_version: String by project
val flyway_version: String by project
val jooq_version: String by project
val mysql_version: String by project
val koin_version: String by project
val hikari_version: String by project
val config4k_version: String by project

application {
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("com.perpheads.files.ApplicationKt")
}


docker {
    javaApplication {
        baseImage.set("eclipse-temurin:17")
        maintainer.set("Perpheads")
        ports.add(8080)
        jvmArgs.add("-XX:+UseShenandoahGC")
        jvmArgs.add("-XX:MaxRAMPercentage=90")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(kotlin("stdlib-common"))
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.github.config4k:config4k:$config4k_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")
    implementation("org.jooq:jooq:$jooq_version")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-mysql:$flyway_version")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("commons-codec:commons-codec:1.15")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-locations-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-data-conversion:$ktor_version")
    implementation("io.ktor:ktor-server-compression:$ktor_version")
    implementation("io.ktor:ktor-server-caching-headers:$ktor_version")
    implementation("io.ktor:ktor-server-conditional-headers:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-forwarded-header:$ktor_version")
    implementation("io.ktor:ktor-server-partial-content:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    runtimeOnly("org.flywaydb:flyway-gradle-plugin:$flyway_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    runtimeOnly("mysql:mysql-connector-java:$mysql_version")
    jooqGenerator("mysql:mysql-connector-java:$mysql_version")
}

val filesDatabaseHost = System.getenv("FILES_DB_HOST") ?: "localhost"
val filesDatabasePort = System.getenv("FILES_DB_PORT") ?: "3306"
val filesDatabaseDatabase = System.getenv("FILES_DB_DATABASE") ?: "ph_files"
val filesDatabaseUser = System.getenv("FILES_DB_USER") ?: "ph_files"
val filesDatabasePassword = System.getenv("FILES_DB_PASSWORD") ?: ""


flyway {
    url = "jdbc:mysql://${filesDatabaseHost}:${filesDatabasePort}/${filesDatabaseDatabase}"
    user = filesDatabaseUser
    password = filesDatabasePassword
    schemas = arrayOf(filesDatabaseDatabase)
    val locs = mutableListOf("filesystem:$projectDir/src/main/resources/db/migration")
    locations = locs.toTypedArray()
}

jooq {
    version.set(jooq_version)
    configurations {
        create("main") {
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "com.mysql.cj.jdbc.Driver"
                    url = "jdbc:mysql://${filesDatabaseHost}:${filesDatabasePort}/${filesDatabaseDatabase}"
                    user = filesDatabaseUser
                    password = filesDatabasePassword
                }
                generator.apply {
                    name = "org.jooq.codegen.JavaGenerator"
                    database.apply {
                        name = "org.jooq.meta.mysql.MySQLDatabase"
                        inputSchema = filesDatabaseDatabase
                        withOutputSchemaToDefault(true)
                        excludes = "flyway_schema_history"
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "com.perpheads.files.db"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") {
    dependsOn(tasks.named<org.flywaydb.gradle.task.FlywayMigrateTask>("flywayMigrate"))

    inputs.files(fileTree("src/main/resources/db/migration"))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    allInputsDeclared.set(true)
    outputs.cacheIf { true }
}

tasks.getByName("compileKotlin") {
    dependsOn(tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq"))
}


tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jar"))
    classpath(tasks.getByName<Jar>("jar"))
}