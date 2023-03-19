import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode

plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

val kotlinWrappersVersion = "1.0.0-pre.515"

kotlin {
    js(IR) {
        browser {
            webpackTask {
                mode = if (System.getenv("DEVELOPMENT") == "true") Mode.DEVELOPMENT else Mode.PRODUCTION
            }
            binaries.executable()
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(kotlin("stdlib-common"))
    implementation(npm("axios", "0.27.2"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation(enforcedPlatform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${kotlinWrappersVersion}"))
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-styled")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-mui")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-icons")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion")
}


val browserDist by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(browserDist.name, tasks.named("browserDistribution").map { it.outputs.files.files.single() })
}