rootProject.name = "OdinAddon"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://jitpack.io")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8.3"
}

val versions = listOf("1.21.11")

stonecutter {
    create(rootProject) {
        versions(versions)
        vcsVersion = versions.first()
    }
}