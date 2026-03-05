//package com.odtheking.odinaddon.pvgui
//
//import com.odtheking.odin.utils.Color
//import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
//import com.odtheking.odinaddon.pvgui.PVState.member
//import com.odtheking.odinaddon.pvgui.utils.LevelUtils
//import com.odtheking.odinaddon.pvgui.utils.Theme
//import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
//import com.odtheking.odinaddon.pvgui.utils.commas
//import com.odtheking.odinaddon.pvgui.utils.displayName
//import com.odtheking.odinaddon.pvgui.utils.resettableLazy
//import com.odtheking.odinaddon.pvgui.utils.toItemStack
//import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
//import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
//
//object PetsPage : PVPage() {
//    override val name = "Pets"
//    private const val SLOT_SPACING = 4f
//    private const val COLS = 9
//    private const val TEXT_SIZE = 14f
//    private const val INFO_TEXT = 12f
//    private val RARITY_ORDER = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")
//
//    private val pets: List<HypixelData.Pet> by resettableLazy {
//        member?.pets?.pets?.sortedWith(
//            compareBy(
//                { RARITY_ORDER.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: RARITY_ORDER.size },
//                { -LevelUtils.getPetLevel(it.exp, rarity(it), it.type) },
//                { -it.exp },
//            )
//        ) ?: emptyList()
//    }
//
//    private fun rarity(pet: HypixelData.Pet): SkyBlockRarity =
//        SkyBlockRarity.Companion.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON
//
//    private fun selectedIndex(): Int {
//        if (PVState.selectedPet >= 0) return PVState.selectedPet
//        return pets.indexOfFirst { it.active }.takeIf { it >= 0 } ?: -1
//    }
//
//    override fun draw(ctx: RenderContext) {
//        if (pets.isEmpty()) return
//
//        val infoW = w * 0.30f
//        val gridW = w - infoW - PADDING
//        val slotSize = (gridW - PADDING - SLOT_SPACING * (COLS - 1)) / COLS
//        val totalRows = (pets.size + COLS - 1) / COLS
//        val visibleRows = ((h - PADDING * 2f) / (slotSize + SLOT_SPACING)).toInt()
//        PVState.petsScroll = PVState.petsScroll.coerceIn(0, (totalRows - visibleRows).coerceAtLeast(0))
//
//        SlotGrid(
//            items = pets,
//            cols = COLS,
//            spacing = SLOT_SPACING,
//            toStack = { it.toItemStack() },
//            itemBg = { pet ->
//                when {
//                    pet.active -> Color(0, 180, 70).rgba
//                    ProfileViewerModule.rarityBackgrounds -> Theme.rarityColor(pet.tier)
//                    else -> Theme.btnNormal
//                }
//            },
//            initialScroll = PVState.petsScroll,
//            initialSelected = selectedIndex(),
//            onSelect = { idx, _ -> PVState.selectedPet = if (PVState.selectedPet == idx) -1 else idx },
//            onScroll = { PVState.petsScroll = it },
//        ).also {
//            it.setBounds(x + PADDING, y + PADDING, gridW - PADDING * 2f, h - PADDING * 2f)
//            it.draw(ctx)
//        }
//
//        Renderer.line(x + gridW + PADDING, y + 4f, x + gridW + PADDING, y + h - 4f, 1f, Theme.separator)
//        drawPanel(ctx, x + gridW + PADDING * 2f, y, infoW - PADDING, h)
//    }
//
//    private fun drawPanel(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float) {
//        val idx = selectedIndex()
//        Renderer.hollowRect(x, y, w, h, 1f, Theme.border, Theme.radius)
//
//        if (idx < 0 || idx >= pets.size) {
//            val msg = "No active pet"
//            Renderer.text(msg, x + (w - Renderer.textWidth(msg, INFO_TEXT)) / 2f, y + h / 2f - INFO_TEXT / 2f, INFO_TEXT, Theme.textSecondary)
//            return
//        }
//
//        val pet = pets[idx]
//        val rar = rarity(pet)
//        val level = LevelUtils.getPetLevel(pet.exp, rar, pet.type).toInt()
//        val progress = LevelUtils.getPetProgress(pet.exp, rar, pet.type)
//        val prefix = Theme.rarityPrefix(pet.tier)
//        var cy = y + PADDING
//
//        val name = "$prefix${pet.displayName}"
//        Renderer.formattedText(name, x + (w - Renderer.formattedTextWidth(name, INFO_TEXT + 1f)) / 2f, cy, INFO_TEXT + 1f)
//        cy += INFO_TEXT + 6f
//
//        Renderer.formattedText("§7Level: $prefix$level", x + PADDING, cy, INFO_TEXT)
//        cy += INFO_TEXT + 4f
//
//        ProgressBar(progress).also {
//            it.setBounds(x + PADDING, cy, w - PADDING * 2f, 6f)
//            it.draw(ctx)
//        }
//        cy += 10f
//
//        Renderer.formattedText("§7XP: §f${pet.exp.toLong().commas}", x + PADDING, cy, TEXT_SIZE)
//        cy += TEXT_SIZE + 8f
//
//        Renderer.line(x + PADDING, cy, x + w - PADDING, cy, 1f, Theme.separator)
//        cy += 8f
//
//        pet.heldItem?.let { id ->
//            Renderer.formattedText("§7Held Item:", x + PADDING, cy, TEXT_SIZE)
//            cy += TEXT_SIZE + 4f
//            val size = (w - PADDING * 2f).coerceAtMost(48f)
//            ItemSlot(stack = RepoItemsAPI.getItem(id), showTooltip = true, bg = 0).also {
//                it.setBounds(x + (w - size) / 2f, cy, size, size)
//                it.draw(ctx)
//            }
//            cy += size + 8f
//        }
//
//        if (pet.candyUsed > 0) {
//            Renderer.formattedText("§7Candy: §6${pet.candyUsed}§7/10", x + PADDING, cy, TEXT_SIZE)
//            cy += TEXT_SIZE + 4f
//        }
//
//        if (pet.active) Renderer.formattedText("§a● Active", x + PADDING, cy, TEXT_SIZE)
//    }
//}