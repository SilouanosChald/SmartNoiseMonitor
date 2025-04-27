pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io") // ✅ Add this if it's missing
    }

    plugins {
        id("com.android.application") version "8.3.1"
        id("com.google.gms.google-services") version "4.4.0"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)

    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven { url = uri("https://jitpack.io") } // <-- ADD THIS

    }

}

rootProject.name = "SmartNoiseMonitor"
include(":app")
