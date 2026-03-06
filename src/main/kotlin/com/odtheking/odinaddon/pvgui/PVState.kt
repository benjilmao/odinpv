package com.odtheking.odinaddon.pvgui

import com.mojang.authlib.GameProfile
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odinaddon.pvgui.pages.DungeonsPage
import com.odtheking.odinaddon.pvgui.pages.InventoryPage
import com.odtheking.odinaddon.pvgui.pages.OverviewPage
import com.odtheking.odinaddon.pvgui.pages.PetsPage
import com.odtheking.odinaddon.pvgui.pages.ProfilePage
import com.odtheking.odinaddon.pvgui.utils.ResettableLazy
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.api.RequestUtils
import kotlinx.coroutines.launch
import java.util.UUID

object PVState {
    val pages: List<PVPage> = listOf(OverviewPage, ProfilePage, DungeonsPage, InventoryPage, PetsPage)
    var currentPage: PVPage = pages.first()

    var statusText: String = "Loading..."
    var profileName: String? = null

    var player: HypixelData.PlayerInfo? = null
        set(value) { field = value; ResettableLazy.resetAll() }

    var petsScroll: Int = 0
    var selectedPet: Int = -1

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

    var scale = 1f
    var originX = 0f
    var originY = 0f
    var mouseX = 0.0
    var mouseY = 0.0

    fun isHovered(x: Float, y: Float, w: Float, h: Float) =
        mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h

    fun fullReset() {
        player = null
        statusText = "Loading..."
        profileName = null
        currentPage = pages.first()
        petsScroll = 0
        selectedPet = -1
        InventoryPage.resetState()
    }

    fun softReset() {
        currentPage = pages.first()
        petsScroll = 0
        selectedPet = -1
        InventoryPage.resetState()
        currentPage.onOpen()
    }

    fun invalidate() {
        ResettableLazy.resetAll()
        InventoryPage.resetState()
        petsScroll = 0
        selectedPet = -1
    }

    fun loadPlayer(name: String) = scope.launch {
        val alreadyLoaded = player?.name?.equals(name, ignoreCase = true) == true
        if (alreadyLoaded) {
            softReset()
            return@launch
        }

        fullReset()
        statusText = "Loading $name..."
        RequestUtils.getProfile(name).fold(
            onSuccess = { data ->
                if (data.profileData.profiles.isEmpty()) {
                    statusText = "No profiles found for ${data.name}."
                } else {
                    player = data
                    profileName = data.profileData.profiles.find { it.selected }?.cuteName
                        ?: data.profileData.profiles.firstOrNull()?.cuteName
                }
            },
            onFailure = { e ->
                modMessage(e.message ?: "Unknown error")
                statusText = "Failed to load profile."
            }
        )
    }
}