pluginManagement {
    repositories {
        google() // Sin filtros para incluir todas las dependencias de Google
        mavenCentral()
        gradlePluginPortal()

    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "dagger.hilt.android.plugin") {
                useModule("com.google.dagger:hilt-android-gradle-plugin:2.48.1")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")

    }
}

rootProject.name = "edukrd"
include(":app")

