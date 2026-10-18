// Why we use this: Configures where Gradle downloads Android plugins and libraries from.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Why we use this: Enforces that all project modules fetch dependencies from Google and Maven Central.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Why we use this: Sets the GitHub repository / project name and registers the main ":app" folder.
rootProject.name = "HyunWears"
include(":app")
