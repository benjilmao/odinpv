@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.loom)
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
}

val mc = stonecutter.current.version

group = rootProject.property("maven_group")!!
version = rootProject.property("mod_version")!!

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.teamresourceful.com/repository/maven-public/")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.language.kotlin)
    modImplementation(libs.fabric.api)

    implementation(libs.kotlinx.serialization.json)

    modRuntimeOnly(libs.devauth)
    modImplementation(libs.odin)
    modImplementation(libs.commodore)

    modImplementation(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${mc}-remapped") }
    }
    include(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${mc}-remapped") }
    }

    modImplementation(libs.olympus)
    include(libs.olympus)

    modImplementation(libs.resourcefullib)
    include(libs.resourcefullib)

    modImplementation(libs.resourcefulconfig)
    include(libs.resourcefulconfig)

    modImplementation(libs.placeholders)
    include(libs.placeholders)

    modImplementation(libs.skyblockapi) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${mc}") }
    }
    include(libs.skyblockapi) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${mc}-remapped") }
    }

    modImplementation(libs.lwjgl.nanovg)
    listOf("windows", "linux", "linux-arm64", "macos", "macos-arm64").forEach { os ->
        modImplementation("org.lwjgl:lwjgl-nanovg:${libs.versions.lwjgl.get()}:natives-$os")
    }
}

loom {
    runConfigs.named("client") {
        isIdeConfigGenerated = true
        runDir = "../../run"
        vmArgs.addAll(arrayOf(
            "-Dmixin.debug.export=true",
            "-Ddevauth.enabled=true",
            "-Ddevauth.account=main",
            "-XX:+AllowEnhancedClassRedefinition"
        ))
    }
    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

afterEvaluate {
    loom.runs.named("client") {
        vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
    }
}

tasks {
    processResources {
        val props = mapOf(
            "mod_id" to rootProject.property("mod_id"),
            "mod_name" to rootProject.property("mod_name"),
            "mod_description" to rootProject.property("mod_description"),
            "mod_version" to rootProject.property("mod_version"),
            "minecraft_version" to mc,
            "loader_version" to libs.versions.fabric.loader.get(),
            "fabric_api_version" to libs.versions.fabric.api.get(),
            "fabric_kotlin_version" to libs.versions.fabric.language.kotlin.get(),
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xlambdas=class") // Commodore
        }
    }

    compileJava {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

base {
    archivesName.set(rootProject.property("archives_base_name") as String)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.odtheking"
            artifactId = "odin-pv"
            version = project.version.toString()
            from(components["java"])
        }
    }
}