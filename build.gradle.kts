// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Declare the plugins and their versions. `apply false` means they are not applied
    // to the root project itself, but are made available for sub-projects to use.
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
