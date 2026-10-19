// Why we use this: Declares the core Android, Kotlin, and Google Services (Firebase) plugins.
plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    // Why we use this: Reads your google-services.json file to connect the app to Firebase.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
