rootProject.name = "ph-files"

pluginManagement {
    resolutionStrategy {
        repositories {
            gradlePluginPortal()
        }
    }
}

include("shared")
include("client")
include("server")