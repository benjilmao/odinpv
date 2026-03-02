package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odinaddon.pvgui.PADDING
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.components.Button
import com.odtheking.odinaddon.pvgui.components.Buttons
import com.odtheking.odinaddon.pvgui.components.DropDown
import com.odtheking.odinaddon.pvgui.components.ItemSlot
import com.odtheking.odinaddon.pvgui.components.ProgressBar
import com.odtheking.odinaddon.pvgui.components.Separator
import com.odtheking.odinaddon.pvgui.components.SlotGrid
import com.odtheking.odinaddon.pvgui.components.TextBox
import com.odtheking.odinaddon.pvgui.components.asText
import com.odtheking.odinaddon.pvgui.components.withButton
import com.odtheking.odinaddon.pvgui.components.withItem
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import net.minecraft.world.item.ItemStack

object TestPage : PVPage() {
    override val name = "Test"

    private val items by resettableLazy {
        member?.inventory?.invContents?.itemStacks.orEmpty().take(36)
    }

    private val toggleBtn = Button("Toggle", textSize = 14f)
    private val optionBtns = Buttons(items = listOf("Option A", "Option B", "Option C"), spacing = 6f, textSize = 14f, vertical = false, label = { it })
    private val tabBtns = Buttons(items = listOf("Tab 1", "Tab 2", "Tab 3"), spacing = 6f, textSize = 13f, vertical = true, label = { it })
    private val dropdown = DropDown(rowH = 30f, textSize = 14f).apply { onSelect { } }

    override fun draw(ctx: RenderContext) {
        val colW = w / 3f

        var cy = y + PADDING
        Renderer.formattedText("§6Buttons", x + PADDING, cy, 16f); cy += 20f
        toggleBtn.setBounds(x + PADDING, cy, colW - PADDING * 2f, 28f); toggleBtn.draw(ctx); cy += 34f
        optionBtns.setBounds(x + PADDING, cy, colW - PADDING * 2f, 28f); optionBtns.draw(ctx); cy += 40f
        tabBtns.setBounds(x + PADDING, cy, 70f, 100f); tabBtns.draw(ctx); cy += 110f

        Renderer.formattedText("§6Progress Bars", x + PADDING, cy, 16f); cy += 20f
        ProgressBar(0.65f).also { it.setBounds(x + PADDING, cy, colW - PADDING * 2f, 8f); it.draw(ctx) }; cy += 14f
        ProgressBar(0.3f, fillColor = 0xFFFF5555.toInt()).also { it.setBounds(x + PADDING, cy, colW - PADDING * 2f, 8f); it.draw(ctx) }; cy += 20f

        Renderer.formattedText("§6Separators", x + PADDING, cy, 16f); cy += 20f
        Separator(vertical = false).also { it.setBounds(x + PADDING, cy, colW - PADDING * 2f, 10f); it.draw(ctx) }; cy += 20f
        Separator(vertical = true).also { it.setBounds(x + PADDING, cy, 10f, 60f); it.draw(ctx) }

        val c2x = x + colW
        cy = y + PADDING
        Renderer.formattedText("§6TextBox", c2x + PADDING, cy, 16f); cy += 20f
        TextBox(listOf(
            "§7Plain text line".asText(),
            "§aWith button".withButton("Click", onClick = {}, textSize = 13f),
            "§bWith item".withItem(items.firstOrNull()?.asItemStack ?: ItemStack.EMPTY),
        ), maxSize = 14f).also { it.setBounds(c2x + PADDING, cy, colW - PADDING * 2f, 120f); it.draw(ctx) }; cy += 130f

        Renderer.formattedText("§6DropDown", c2x + PADDING, cy, 16f); cy += 20f
        dropdown.setBounds(c2x + PADDING, cy, colW - PADDING * 2f, 30f)
        dropdown.draw(ctx, "Select profile", null, listOf("Profile 1", "Profile 2", "Profile 3").mapIndexed { i, s -> Triple(s, null as String?, i == 0) })

        val c3x = x + colW * 2f
        cy = y + PADDING
        Renderer.formattedText("§6ItemSlot", c3x + PADDING, cy, 16f); cy += 20f
        ItemSlot(items.firstOrNull()?.asItemStack ?: ItemStack.EMPTY).also { it.setBounds(c3x + PADDING, cy, 40f, 40f); it.draw(ctx) }; cy += 50f

        Renderer.formattedText("§6SlotGrid", c3x + PADDING, cy, 16f); cy += 20f
        SlotGrid(items = items, cols = 9, spacing = 4f, toStack = { it?.asItemStack ?: ItemStack.EMPTY }).also {
            it.setBounds(c3x + PADDING, cy, colW - PADDING * 2f, h - (cy - y) - PADDING)
            it.draw(ctx)
        }

        Renderer.line(x + colW, y + 4f, x + colW, y + h - 4f, 1f, Theme.separator)
        Renderer.line(x + colW * 2f, y + 4f, x + colW * 2f, y + h - 4f, 1f, Theme.separator)
    }
}
