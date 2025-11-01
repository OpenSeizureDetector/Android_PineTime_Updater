// This file configures which modules are included in the build and where to find their dependencies.

// Configures where Gradle should look for plugins.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Configures where Gradle should look for all project dependencies.
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Standard repositories for Android projects
        google()
        mavenCentral()
    }
}

rootProject.name = "OSD PineTime DFU"
// Include only the main application module in the build.
include(":app")
