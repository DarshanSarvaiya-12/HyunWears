// Why we use this: Activates the Android application builder and Jetpack Compose compiler for our app.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // Why we use this: Unique package identifier for HyunWears by PiCell Company.
    namespace = "com.picell.hyunwears"
    compileSdk = 36 // Target Android 16 (2026 standard)

    defaultConfig {
        applicationId = "com.picell.hyunwears"
        minSdk = 26 // Supports 95%+ of active Android phones
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // Why we use this: Enables Jetpack Compose so we can build the UI entirely in Kotlin code.
    buildFeatures {
        compose = true
    }

    // Why we use this: Uses Java 17, which is required by 2026 Android Gradle build tools.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Why we use this: The 08/2026 Compose BOM automatically manages compatible versions for all Compose UI libraries.
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)

    // Why we use this: Core Jetpack Compose libraries for modern Light Mode Material3 design, layouts, and icons.
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Why we use this: Coil library loads high-resolution T-shirt images smoothly from the internet.
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")
}
