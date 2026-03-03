package com.odtheking.odinaddon.pvgui

import com.mojang.authlib.GameProfile
import com.odtheking.odinaddon.pvgui.pages.DungeonsPage
import com.odtheking.odinaddon.pvgui.pages.InventoryPage
import com.odtheking.odinaddon.pvgui.pages.OverviewPage
import com.odtheking.odinaddon.pvgui.pages.PetsPage
import com.odtheking.odinaddon.pvgui.pages.ProfilePage
import com.odtheking.odinaddon.pvgui.utils.ResettableLazy
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import java.util.UUID

object PVState {
    var statusText: String = "Loading..."
    var profileName: String? = null
    val pages: List<PVPage> = listOf(OverviewPage, ProfilePage, DungeonsPage, InventoryPage, PetsPage)
    var currentPage: PVPage = pages.first()
    var petsScroll: Int = 0
    var selectedPet: Int = -1
    var player: HypixelData.PlayerInfo? = null
        set(value) {
            field = value
            ResettableLazy.resetAll()
        }

    fun reset() {
        player = null
        statusText = "Loading..."
        profileName = null
        currentPage = pages.first()
        petsScroll = 0
        selectedPet = -1
        InventoryPage.resetState()
        OverviewPage.onOpen()
    }

    fun invalidate() {
        ResettableLazy.resetAll()
        InventoryPage.resetState()
        petsScroll = 0
        selectedPet = -1
    }

    fun profile() = player?.profileData?.profiles?.find { it.cuteName == profileName }
        ?: player?.profileData?.profiles?.find { it.selected }
        ?: player?.profileData?.profiles?.firstOrNull()

    fun member() = profile()?.members?.get(player?.uuid)

    val playerGameProfile: GameProfile? get() {
        val uuid = player?.uuid ?: return null
        val parsed = runCatching {
            val u = uuid.replace("-", "")
            UUID.fromString("${u.take(8)}-${u.substring(8,12)}-${u.substring(12,16)}-${u.substring(16,20)}-${u.substring(20)}")
        }.getOrNull() ?: return null
        return GameProfile(parsed, player?.name ?: "")
    }
}