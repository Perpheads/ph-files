plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    js(IR) {
        browser {
            binaries.executable()
        }
    }

    kotlin {
        sourceSets {
            val commonMain by getting {
                dependencies {
                    implementation(kotlin("stdlib-common"))
                    api("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
                }
            }
            val jsMain by getting
            val jvmMain by getting
        }
    }
}
