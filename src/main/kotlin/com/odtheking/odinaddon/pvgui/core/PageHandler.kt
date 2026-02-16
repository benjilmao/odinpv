package com.odtheking.odinaddon.pvgui.core

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.components.Box
import com.odtheking.odinaddon.pvgui.components.buttons
import com.odtheking.odinaddon.pvgui.utils.*
import net.minecraft.client.gui.GuiGraphics

object PageHandler {
    private var currentPage = PageEntries.Overview
    private val infoHeight = 60f

    private val sidebarButtonsTotalHeight by resettableLazy {
        PageData.mainHeight - infoHeight - PageData.spacer
    }

    private val pageButtons by resettableLazy {
        buttons(
            box = Box(
                PageData.spacer.toFloat(),
                PageData.spacer.toFloat(),
                (PageData.sidebarWidth - 2 * PageData.spacer).toFloat(),
                sidebarButtonsTotalHeight
            ),
            padding = PageData.spacer,
            default = PageEntries.entries.first().page.name,
            options = PageEntries.entries.map { it.page.name },
            textScale = 2f * PageData.scale,
            color = Theme.buttonBg.rgba,
            selectedColor = Theme.buttonSelected.rgba,
            radius = Theme.roundness,
            vertical = true
        ) {
            onSelect { pageName ->
                currentPage = PageEntries.entries.find { it.page.name == pageName } ?: return@onSelect
            }
        }
    }

    fun preDraw(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVGui.playerData == null) {
            renderLoadingScreen()
            return
        }

        NVGRenderer.rect(
            0f, 0f,
            PageData.totalWidth.toFloat(), PageData.totalHeight.toFloat(),
            Theme.bgColor.rgba, Theme.roundness
        )

        pageButtons.draw(mouseX, mouseY)

        NVGRenderer.rect(
            PageData.sidebarWidth.toFloat(), PageData.spacer.toFloat(),
            1f, (PageData.totalHeight - 2 * PageData.spacer).toFloat(),
            Theme.lineColor.rgba, 0f
        )

        val infoPanelY = PageData.totalHeight - infoHeight - PageData.spacer
        NVGRenderer.rect(
            PageData.spacer.toFloat(),
            infoPanelY,
            (PageData.sidebarWidth - 2 * PageData.spacer).toFloat(),
            infoHeight,
            Theme.secondaryBg.rgba,
            Theme.roundness
        )

        val versionText = "PV v2.0"
        val textSize = 12f
        val textWidth = NVGRenderer.textWidth(versionText, textSize, NVGRenderer.defaultFont)
        val versionX = PageData.spacer + ((PageData.sidebarWidth - 2 * PageData.spacer - textWidth) / 2)
        val versionY = infoPanelY + (infoHeight / 2) - (textSize / 2)

        NVGRenderer.text(
            versionText,
            versionX,
            versionY,
            textSize,
            Theme.fontColor.rgba,
            NVGRenderer.defaultFont
        )

        currentPage.page.draw(guiGraphics, mouseX, mouseY)
    }

    private fun renderLoadingScreen() {
        NVGRenderer.rect(
            0f, 0f,
            PageData.totalWidth.toFloat(), PageData.totalHeight.toFloat(),
            Theme.bgColor.rgba, Theme.roundness
        )

        val textSize = 20f
        val textWidth = NVGRenderer.textWidth(PVGui.loadText, textSize, NVGRenderer.defaultFont)
        NVGRenderer.text(
            PVGui.loadText,
            PageData.totalWidth / 2f - textWidth / 2,
            PageData.totalHeight / 2f,
            textSize,
            0xFFAAAAAA.toInt(),
            NVGRenderer.defaultFont
        )
    }

    fun handleClick(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (PVGui.playerData == null) return
        if (pageButtons.click(mouseX, mouseY, mouseButton)) return
        currentPage.page.click(mouseX, mouseY, mouseButton)
    }

    fun reset() {
        currentPage = PageEntries.Overview
    }
}