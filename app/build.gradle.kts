plugins {
    alias(libs.plugins.nordic.application.compose)
    alias(libs.plugins.nordic.hilt)
    // Add the Kotlinx Serialization plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

if (gradle.startParameter.taskRequests.toString().contains("Release")) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "uk.org.openseizuredetector.pinetime"
    defaultConfig {
        minSdk = 23
    }
    androidResources {
        localeFilters += setOf("en")
    }
}

dependencies {
    implementation(project(":lib:dfu"))

    implementation(libs.androidx.activity.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Dependencies for Networking and JSON Parsing
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    // Use the official converter
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Add Material Components for theme resolution
    implementation("com.google.android.material:material:1.11.0")
}
