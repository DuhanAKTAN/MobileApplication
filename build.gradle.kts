// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false

}
buildscript {
    dependencies {
        // Android Gradle Plugin
        classpath("com.android.tools.build:gradle:8.1.0")
        // Google Services Plugin
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.google.gms:google-services:4.3.10")
        classpath("com.android.tools.build:gradle:7.0.4")
    }
}

