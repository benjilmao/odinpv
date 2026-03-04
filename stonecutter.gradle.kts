plugins {
    id("dev.kikugie.stonecutter")
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

stonecutter active "1.21.10"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod_version")}\";"
    swaps["minecraft_version"] = "\"${node.metadata.version}\";"
}