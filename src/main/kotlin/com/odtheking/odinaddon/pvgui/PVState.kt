package com.odtheking.odinaddon.pvgui

import com.odtheking.odinaddon.pvgui.pages.DungeonsPage
import com.odtheking.odinaddon.pvgui.pages.InventoryPage
import com.odtheking.odinaddon.pvgui.pages.OverviewPage
import com.odtheking.odinaddon.pvgui.pages.PetsPage
import com.odtheking.odinaddon.pvgui.pages.ProfilePage
import com.odtheking.odinaddon.pvgui.utils.ResettableLazy
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID

object PVState {
    var loadText: String = "Loading..."
    var profileName: String? = null
    var skinLocation: ResourceLocation? = null
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
        skinLocation = null
        playerData = null
        loadText = "Loading..."
        profileName = null
        currentPage = pages.first()
        petsScroll = 0
        selectedPetIndex = -1
        OverviewPage.resetDropdown()
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

    fun getPlayerHeadItem(uuid: String): ItemStack {
        val stack = ItemStack(Items.PLAYER_HEAD)
        val javaUuid = runCatching {
            val u = uuid.replace("-", "")
            UUID.fromString("${u.take(8)}-${u.substring(8,12)}-${u.substring(12,16)}-${u.substring(16,20)}-${u.substring(20)}")
        }.getOrNull() ?: return stack
        stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(javaUuid))
        return stack
    }
}