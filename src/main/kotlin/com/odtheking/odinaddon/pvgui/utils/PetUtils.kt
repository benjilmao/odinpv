package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.PetQuery
import tech.thatgravyboat.skyblockapi.api.remote.RepoPetsAPI
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI

val HypixelData.Pet.displayName: String get() =
    type.lowercase().split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

val HypixelData.Pet.coloredName: String get() = "${Theme.petTierColor(tier)}$displayName"

val HypixelData.Pet.heldItemStack: ItemStack? get() = heldItem?.let { RepoItemsAPI.getItem(it) }

fun HypixelData.Pet.toItemStack(): ItemStack {
    val rarity = SkyBlockRarity.fromNameOrNull(tier) ?: SkyBlockRarity.COMMON
    return RepoPetsAPI.getPetAsItem(PetQuery(type, rarity, LevelUtils.getPetLevel(exp, rarity, type).toInt(), skin, heldItem))
}