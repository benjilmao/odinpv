package com.odtheking.odinaddon.pvgui

import com.odtheking.odinaddon.pvgui.utils.HypixelData

object PVState {
    var playerData: HypixelData.PlayerInfo? = null
    var loadText: String = "Loading..."
    var profileName: String? = null
    var currentPage: String = "Overview"
    val pages = listOf("Overview", "Profile", "Dungeons", "Inventory", "Pets")
    var inventoryScroll: Int = 0
    var petsScroll: Int = 0
    var selectedPetIndex: Int = -1
    var inventorySubPage: String = "Basic"

    fun reset() {
        playerData = null
        loadText = "Loading..."
        profileName = null
        currentPage = "Overview"
        inventoryScroll = 0
        petsScroll = 0
        selectedPetIndex = -1
        inventorySubPage = "Basic"
    }

    fun selectedProfile() = playerData?.profileData?.profiles?.find { it.cuteName == profileName }
        ?: playerData?.profileData?.profiles?.find { it.selected }
        ?: playerData?.profileData?.profiles?.firstOrNull()

    fun memberData() = selectedProfile()?.members?.get(playerData?.uuid)
}