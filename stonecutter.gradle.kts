plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.15-SNAPSHOT" apply false
    kotlin("jvm") version "2.3.10" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
}

stonecutter active "1.21.11"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod_version")}\";"
    swaps["minecraft_version"] = "\"${node.metadata.version}\";"
}