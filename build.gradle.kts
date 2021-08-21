import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.gradle.task.FlywayMigrateTask

plugins {
    kotlin("multiplatform") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
    id("org.flywaydb.flyway") version "7.14.0"
    id("nu.studer.jooq") version "6.0"
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
val kotlin_react_version: String by project

group = "com.perpheads"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.perpheads.files.ApplicationKt")
}

dependencies {
    runtimeOnly("mysql:mysql-connector-java:$mysql_version")
    jooqGenerator("mysql:mysql-connector-java:$mysql_version")
}

val filesDatabaseHost = System.getenv("FILES_DB_HOST") ?: "localhost"
val filesDatabasePort = System.getenv("FILES_DB_PORT") ?: "3306"
val filesDatabaseDatabase = System.getenv("FILES_DB_DATABASE") ?: "ph_files"
val filesDatabaseUser = System.getenv("FILES_DB_USER") ?: "ph_files"
val filesDatabasePassword = System.getenv("FILES_DB_PASSWORD") ?: ""


kotlin {
    jvm {
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
            }
        }

        val jvmMain by getting {
            languageSettings {
                useExperimentalAnnotation("io.ktor.locations.KtorExperimentalLocationsAPI")
            }

            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-auth:$ktor_version")
                implementation("io.ktor:ktor-locations:$ktor_version")
                implementation("io.ktor:ktor-websockets:$ktor_version")
                implementation("io.ktor:ktor-server-host-common:$ktor_version")
                implementation("io.ktor:ktor-html-builder:$ktor_version")
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")
                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation("io.github.config4k:config4k:$config4k_version")
                implementation("com.zaxxer:HikariCP:$hikari_version")
                implementation("org.jooq:jooq:$jooq_version")
                implementation("org.flywaydb:flyway-core:$flyway_version")
                implementation("org.mindrot:jbcrypt:0.4")
                implementation("org.imgscalr:imgscalr-lib:4.2")
                implementation("commons-codec:commons-codec:1.15")
                runtimeOnly("org.flywaydb:flyway-gradle-plugin:$flyway_version")
                implementation("io.insert-koin:koin-ktor:$koin_version")
                runtimeOnly("mysql:mysql-connector-java:$mysql_version")
            }
        }

        fun kotlinw(target: String, version: String): String =
            "org.jetbrains.kotlin-wrappers:kotlin-$target:$version-pre.233-kotlin-1.5.21"


        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-js:$ktor_version")
                implementation("io.ktor:ktor-client-serialization:$ktor_version")
                implementation(kotlin("stdlib-common"))
                implementation(kotlinw("react", "17.0.2"))
                implementation(kotlinw("react-dom", "17.0.2"))
                implementation(kotlinw("styled", "5.3.0"))
                implementation(kotlinw("react-router-dom", "5.2.0"))
            }
        }
    }
}


flyway {
    url = "jdbc:mysql://${filesDatabaseHost}:${filesDatabasePort}/${filesDatabaseDatabase}"
    user = filesDatabaseUser
    password = filesDatabasePassword
    schemas = arrayOf(filesDatabaseDatabase)
    val locs = mutableListOf("filesystem:$projectDir/src/jvmMain/resources/db/migration")
    locations = locs.toTypedArray()
}

jooq {
    version.set(jooq_version)
    configurations {
        create("jvmMain") {
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
                        directory = "src/jvmMain/java"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

tasks.named<JooqGenerate>("generateJvmMainJooq") {
    dependsOn(tasks.named<FlywayMigrateTask>("flywayMigrate"))

    inputs.files(fileTree("src/jvmMain/resources/db/migration"))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    allInputsDeclared.set(true)
    outputs.cacheIf { true }
}

tasks.getByName("compileKotlinJvm") {
    dependsOn(tasks.named<JooqGenerate>("generateJvmMainJooq"))
}

tasks.withType<AbstractCopyTask> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
    classpath(tasks.getByName<Jar>("jvmJar"))
}