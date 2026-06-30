plugins {
    id("com.android.application") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}


task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}

buildscript {
    dependencies {
        classpath ("com.google.gms:google-services:4.4.0")
        classpath ("com.android.tools.build:gradle:8.1.0")
        // ... outros classpaths
    }
}