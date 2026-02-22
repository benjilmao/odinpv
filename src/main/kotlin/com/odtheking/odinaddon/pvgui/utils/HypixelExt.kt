package com.odtheking.odinaddon.pvgui.utils

val HypixelData.PlayerInfo.profileList: List<Pair<String, String>>
    get() = profileData.profiles.map {
        (it.cuteName ?: "Unknown") to (it.gameMode?.ifEmpty { "normal" } ?: "normal")
    }

fun HypixelData.PlayerInfo.profileOrSelected(selected: String?): HypixelData.Profiles? =
    profileData.profiles.find { it.cuteName == selected } ?: profileData.profiles.find { it.selected }