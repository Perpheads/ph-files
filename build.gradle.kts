import com.github.dockerjava.core.dockerfile.Dockerfile
import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.gradle.task.FlywayMigrateTask
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
    id("org.flywaydb.flyway") version "7.14.0"
    id("nu.studer.jooq") version "6.0"
    id("com.bmuschko.docker-java-application") version "7.1.0"
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
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("com.perpheads.files.ApplicationKt")
}

docker {
    javaApplication {
        baseImage.set("adoptopenjdk:11")
        maintainer.set("Perpheads")
        ports.add(8080)
        jvmArgs.add("-XX:+UseShenandoahGC")
        jvmArgs.add("-XX:MaxRAMPercentage=90")
    }
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
                optIn("io.ktor.locations.KtorExperimentalLocationsAPI")
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
                implementation("io.ktor:ktor-html-builder:$ktor_version")
                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation("io.github.config4k:config4k:$config4k_version")
                implementation("com.zaxxer:HikariCP:$hikari_version")
                implementation("org.jooq:jooq:$jooq_version")
                implementation("org.flywaydb:flyway-core:$flyway_version")
                implementation("org.mindrot:jbcrypt:0.4")
                implementation("commons-codec:commons-codec:1.15")
                runtimeOnly("org.flywaydb:flyway-gradle-plugin:$flyway_version")
                implementation("io.insert-koin:koin-ktor:$koin_version")
                runtimeOnly("mysql:mysql-connector-java:$mysql_version")
            }
        }

        val kotlinWrappersVersion = "0.0.1-pre.237-kotlin-1.5.30"

        val jsMain by getting {
            languageSettings {
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(npm("axios", "0.21.1"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${kotlinWrappersVersion}")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-styled")
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

tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "ph-files.js"
    sourceMaps = true
}


tasks.withType<AbstractCopyTask> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.getByName("dockerCreateDockerfile") {
    dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
    doLast {
        copy {
            from("$buildDir/distributions/ph-files.js")
            into("$buildDir/docker/resources/")
        }
    }
}

tasks.getByName<Jar>("jvmJar") {
    dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
    val jsBrowserProductionWebpack = tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack")

    listOf(jsBrowserProductionWebpack.outputFileName, jsBrowserProductionWebpack.outputFileName + ".map").forEach {
        from(File(jsBrowserProductionWebpack.destinationDirectory, it))
    }
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