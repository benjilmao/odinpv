package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.dsl.ItemQueue
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.dsl.formattedText
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.platform.toResolvableProfile

object ItemTestPage : PVPage() {
    override val name = "Test Items"

    private val PAD    = 12f
    private val RADIUS = 8f

    private val gridItems: List<ItemStack> by lazy {
        listOf(
            ItemStack(Items.DIAMOND,           64),
            ItemStack(Items.EMERALD,           32),
            ItemStack(Items.NETHER_STAR,        1),
            ItemStack(Items.TOTEM_OF_UNDYING,   1),
            ItemStack(Items.ELYTRA,             1),
            ItemStack(Items.GOLD_INGOT,        48),
            ItemStack(Items.IRON_INGOT,        16),
            ItemStack(Items.OBSIDIAN,          64),
            ItemStack(Items.BLAZE_ROD,          7),
            ItemStack(Items.ENDER_PEARL,       16),
            ItemStack(Items.GHAST_TEAR,         3),
            ItemStack(Items.ENCHANTED_BOOK,     1),
            ItemStack(Items.NETHERITE_INGOT,    1),
            ItemStack(Items.DRAGON_EGG,         1),
            ItemStack(Items.BEACON,             1),
            ItemStack(Items.HEART_OF_THE_SEA,   1),
            ItemStack(Items.CONDUIT,            1),
            ItemStack(Items.SHULKER_SHELL,      9),
        )
    }

    private val playerHead: ItemStack by lazy {
        ItemStack(Items.PLAYER_HEAD).also { stack ->
            mc.player?.gameProfile?.let { gp ->
                val skyblockProfile = com.mojang.authlib.GameProfile(gp.id, gp.name)
                stack.set(DataComponents.PROFILE, skyblockProfile.toResolvableProfile())
            }
        }
    }

    private val infoLines = listOf(
        "§bPV §fGUI §7— item rendering",
        "§7Hover any item for §ftooltip",
        "§7Stack counts render §fnatively",
        "§7Player head: §fyour skin",
        "§aScale independent §7at any guiScale",
    )

    override fun draw() {
        val font  = NVGRenderer.defaultFont
        val leftW = w * 0.30f
        val leftH = h - PAD * 2f
        val lx    = x + PAD
        val ly    = y + PAD

        // Left info panel
        NVGRenderer.rect(lx, ly, leftW, leftH, Theme.bg, RADIUS)
        NVGRenderer.rect(lx, ly, 3f, leftH, Theme.btnSelected, RADIUS)
        NVGRenderer.text("Overview", lx + PAD + 3f, ly + PAD, 17f, Theme.textPrimary, font)
        NVGRenderer.rect(lx, ly + PAD + 26f, leftW, 1f, Theme.separator)

        TextBox(x = lx + PAD + 3f, y = ly + PAD + 32f,
            w = leftW - PAD * 2f, h = leftH * 0.38f,
            lines = infoLines, textSize = 13f).draw()

        formattedText("§7guiScale: §f${mc.options.guiScale().get()}", lx + PAD + 3f, ly + leftH * 0.48f, 12f)

        NVGRenderer.rect(lx, ly + leftH * 0.52f, leftW, 1f, Theme.separator)
        NVGRenderer.text("Your Head", lx + PAD + 3f, ly + leftH * 0.54f, 14f, Theme.textSecondary, font)

        val headSize = minOf(leftW * 0.55f, leftH * 0.28f)
        val headX    = lx + (leftW - headSize) / 2f
        val headY    = ly + leftH * 0.59f
        NVGRenderer.rect(headX - 4f, headY - 4f, headSize + 8f, headSize + 8f, Theme.slotBg, RADIUS)
        ItemQueue.queue(playerHead, headX, headY, headSize, showTooltip = true)

        // Right grid panel
        val rightX = lx + leftW + PAD
        val rightW = w - leftW - PAD * 3f
        val rightH = leftH

        NVGRenderer.rect(rightX, ly, rightW, rightH, Theme.bg, RADIUS)
        NVGRenderer.text("Item Grid", rightX + PAD, ly + PAD, 17f, Theme.textPrimary, font)
        NVGRenderer.rect(rightX, ly + PAD + 26f, rightW, 1f, Theme.separator)

        formattedText(
            "§7${gridItems.size} items  ·  ${gridItems.count { it.count > 1 }} with stacks  ·  hover for tooltips",
            rightX + PAD, ly + PAD + 34f, 12f
        )

        val cols        = 9
        val gridSlotGap = 4f
        val gridX       = rightX + PAD
        val gridY       = ly + PAD + 52f
        val gridW       = rightW - PAD * 2f
        val slotSize    = (gridW - gridSlotGap * (cols - 1)) / cols
        val scissor     = floatArrayOf(rightX, ly, rightX + rightW, ly + rightH)

        gridItems.forEachIndexed { idx, stack ->
            val sx = gridX + (idx % cols) * (slotSize + gridSlotGap)
            val sy = gridY + (idx / cols) * (slotSize + gridSlotGap)
            NVGRenderer.rect(sx, sy, slotSize, slotSize, Theme.slotBg, 4f)
            ItemQueue.queue(stack, sx, sy, slotSize, showTooltip = true, scissor = scissor)
        }

        val rows       = (gridItems.size + cols - 1) / cols
        val gridBottom = gridY + rows * (slotSize + gridSlotGap) + 6f
        formattedText("§8stack counts + durability bars rendered by §7renderItemDecorations", rightX + PAD, gridBottom, 10f)
    }
}
