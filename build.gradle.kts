// Why we use this: Declares the August 2026 versions of Android Gradle Plugin and Kotlin Compiler without applying them to the root folder yet.
plugins {
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
