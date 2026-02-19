import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom")
    kotlin("jvm")
    `maven-publish`
}

group = property("maven_group")!!
version = property("mod_version")!!

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
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
    modImplementation("com.github.odtheking:odinfabric:${property("odin_version")}")

    modImplementation("com.github.stivais:Commodore:${property("commodore_version")}")

    modImplementation("me.owdding.meowdding-lib:meowdding-lib:${property("meowdding_lib_version")}") {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-1.21.10-remapped") }
    }
    include("me.owdding.meowdding-lib:meowdding-lib:${property("meowdding_lib_version")}") {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-1.21.10-remapped") }
    }

    // meowdding-lib's required deps — include them so they ship with your jar
    modImplementation("earth.terrarium.olympus:olympus-fabric-1.21.9:${property("olympus_version")}")
    include("earth.terrarium.olympus:olympus-fabric-1.21.9:${property("olympus_version")}")

    modImplementation("com.teamresourceful.resourcefullib:resourcefullib-fabric-1.21.9:${property("resourcefullib_version")}")
    include("com.teamresourceful.resourcefullib:resourcefullib-fabric-1.21.9:${property("resourcefullib_version")}")
    modImplementation("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.9:${property("resourcefulconfig_version")}")
    include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.9:${property("resourcefulconfig_version")}")

    modImplementation("eu.pb4:placeholder-api:${property("placeholders_version")}")
    include("eu.pb4:placeholder-api:${property("placeholders_version")}")

    // skyblockapi (meowdding-lib depends on it)
    modImplementation("tech.thatgravyboat:skyblock-api:${property("skyblockapi_version")}") {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${property("minecraft_version")}") }
    }
    include("tech.thatgravyboat:skyblock-api:${property("skyblockapi_version")}") {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${property("minecraft_version")}-remapped") }
    }

    property("minecraft_lwjgl_version").let { lwjglVersion ->
        modImplementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion")

        listOf("windows", "linux", "macos", "macos-arm64").forEach { os ->
            modImplementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-$os")
        }
    }
}

loom {
    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.addAll(
            arrayOf(
                "-Dmixin.debug.export=true",
                "-Ddevauth.enabled=true",
                "-Ddevauth.account=main",
                "-XX:+AllowEnhancedClassRedefinition"
            )
        )
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
        filesMatching("fabric.mod.json") {
            expand(getProperties())
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xlambdas=class") //Commodore
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
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)

    withSourcesJar()
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}