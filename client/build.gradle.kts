plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

val kotlinWrappersVersion = "1.0.0-pre.337"

kotlin {
    js(IR) {
        browser {
            binaries.executable()
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(kotlin("stdlib-common"))
    implementation(npm("axios", "0.27.2"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    implementation(enforcedPlatform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${kotlinWrappersVersion}"))
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-styled")
    implementation(npm("@emotion/react", "11.7.1"))
    implementation(npm("@emotion/styled", "11.6.0"))
}


val browserDist by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(browserDist.name, tasks.named("browserDistribution").map { it.outputs.files.files.single() })
}