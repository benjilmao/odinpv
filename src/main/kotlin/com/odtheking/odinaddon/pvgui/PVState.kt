package com.odtheking.odinaddon.pvgui

import com.odtheking.odinaddon.pvgui.pages.DungeonsPage
import com.odtheking.odinaddon.pvgui.pages.InventoryPage
import com.odtheking.odinaddon.pvgui.pages.OverviewPage
import com.odtheking.odinaddon.pvgui.pages.PetsPage
import com.odtheking.odinaddon.pvgui.pages.ProfilePage
import com.odtheking.odinaddon.pvgui.utils.ResettableLazy
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData

object PVState {
    var loadText: String = "Loading..."
    var profileName: String? = null
    val pages: List<PageHandler> = listOf(OverviewPage, ProfilePage, DungeonsPage, InventoryPage, PetsPage)
    var currentPage: PageHandler = pages.first()
    var petsScroll: Int = 0
    var selectedPetIndex: Int = -1
    var playerData: HypixelData.PlayerInfo? = null
        set(value) {
            field = value
            ResettableLazy.resetAll()
        }

    fun reset() {
        playerData = null
        loadText = "Loading..."
        profileName = null
        currentPage = pages.first()
        petsScroll = 0
        selectedPetIndex = -1
        ResettableLazy.resetAll()
    }

    fun invalidateCache() {
        ResettableLazy.resetAll()
        InventoryPage.resetState()
        petsScroll = 0
        selectedPetIndex = -1
    }

    fun selectedProfile() = playerData?.profileData?.profiles?.find { it.cuteName == profileName }
        ?: playerData?.profileData?.profiles?.find { it.selected }
        ?: playerData?.profileData?.profiles?.firstOrNull()

    fun memberData() = selectedProfile()?.members?.get(playerData?.uuid)
}