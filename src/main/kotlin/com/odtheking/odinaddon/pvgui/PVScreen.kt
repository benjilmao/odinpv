package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odin.utils.ui.rendering.NVGSpecialRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui2.utils.RequestUtils
import com.odtheking.odinaddon.pvgui.PVLayout.LOGICAL_H
import com.odtheking.odinaddon.pvgui.PVLayout.LOGICAL_W
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_H
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_W
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_X
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_Y
import com.odtheking.odinaddon.pvgui.PVLayout.PADDING
import com.odtheking.odinaddon.pvgui.PVLayout.SEPARATOR_X
import com.odtheking.odinaddon.pvgui.PVLayout.SIDEBAR_W
import com.odtheking.odinaddon.pvgui.pages.*
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.min

object PVScreen : Screen(Component.literal("Profile Viewer")) {
    private val itemWidgets = mutableListOf<net.minecraft.client.gui.components.AbstractWidget>()

    private const val BTN_SPACING = 6f
    private val BTN_COUNT get() = PVState.pages.size
    private val BTN_H get() = (LOGICAL_H - PADDING * 2f - BTN_SPACING * (BTN_COUNT - 1)) / BTN_COUNT
    private val BTN_W = SIDEBAR_W - PADDING * 2f
    private const val BTN_X = PADDING

    private val COL_GUI_BG get() = ProfileViewerModule.guiBg
    private val COL_BTN_SEL get() = ProfileViewerModule.buttonColor
    private val COL_BTN_HOVER = Color(255, 255, 255, 0.12f)
    private val COL_BTN_NORMAL = Color(255, 255, 255, 0.06f)
    private val COL_SEPARATOR = Color(255, 255, 255, 0.15f)
    private val COL_TEXT_NORMAL = Color(180, 180, 180)
    private val COL_TEXT_SELECT = Color(255, 255, 255)
    private val COL_TEXT_LOAD = Color(170, 170, 170)

    private val GUI_RADIUS  get() = ProfileViewerModule.guiRoundness
    private val BTN_RADIUS  get() = ProfileViewerModule.buttonRoundness
    private const val TEXT_SIZE = 16f

    private var scale = 1f
    private var originX = 0f
    private var originY = 0f
    private var mouseX = 0.0
    private var mouseY = 0.0

    private fun makeCtx() = DrawContext(scale, originX, originY, NVGRenderer.defaultFont, itemWidgets)

    fun loadPlayer(name: String) = scope.launch {
        PVState.reset()
        PVState.loadText = "Loading $name..."
        RequestUtils.getProfile(name).fold(
            onSuccess = { data ->
                if (data.profileData.profiles.isEmpty()) {
                    PVState.loadText = "No profiles found for ${data.name}."
                } else {
                    PVState.playerData = data
                    PVState.profileName =
                        data.profileData.profiles.find { it.selected }?.cuteName
                            ?: data.profileData.profiles.firstOrNull()?.cuteName
                }
            },
            onFailure = { e ->
                modMessage(e.message ?: "Unknown error")
                PVState.loadText = "Failed to load profile."
            }
        )
    }

    override fun init() { super.init() }
    override fun onClose() { PVState.reset(); super.onClose() }
    override fun isPauseScreen() = false

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, bl)
        val ctx = makeCtx()

        PVState.pages.forEachIndexed { i, page ->
            if (ctx.isHovered(mouseX, mouseY, BTN_X, buttonLogicalY(i), BTN_W, BTN_H)) {
                if (PVState.currentPage != page) {
                    PVState.currentPage = page
                    PVState.petsScroll = 0
                    PVState.inventoryScroll = 0
                    PVState.selectedPetIndex = -1
                }
                return true
            }
        }

        currentPageHandler()?.onClick(ctx, mouseX, mouseY)
        return super.mouseClicked(event, bl)
    }

    override fun mouseScrolled(mx: Double, my: Double, hAmount: Double, vAmount: Double): Boolean {
        val ctx = makeCtx()
        if (ctx.isHovered(mouseX, mouseY, MAIN_X, MAIN_Y, MAIN_W, MAIN_H)) {
            currentPageHandler()?.onScroll(vAmount)
        }
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) { mc.setScreen(null); return true }
        return super.keyPressed(event)
    }

    override fun render(context: GuiGraphics, mx: Int, my: Int, delta: Float) {
        recalcScale()

        val dpr = NVGRenderer.devicePixelRatio()
        mouseX = mx * (mc.window.width / dpr) / context.guiWidth().toDouble()
        mouseY = my * (mc.window.height / dpr) / context.guiHeight().toDouble()
        val ctx = makeCtx()

        NVGSpecialRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(originX, originY)
            NVGRenderer.scale(scale, scale)
            drawBackground(ctx)
            drawSidebar(ctx)
            drawMainArea(ctx)
            NVGRenderer.pop()
        }
        super.render(context, mx, my, delta)

        val guiScale = mc.window.guiScale.toFloat()
        val toGuiPx  = dpr / guiScale
        val sx = ((originX + MAIN_X * scale) * toGuiPx).toInt()
        val sy = ((originY + MAIN_Y * scale) * toGuiPx).toInt()
        val sw = (MAIN_W * scale * toGuiPx).toInt()
        val sh = (MAIN_H * scale * toGuiPx).toInt()
        context.enableScissor(sx, sy, sx + sw, sy + sh)
        itemWidgets.forEach { it.render(context, mx, my, delta) }
        context.disableScissor()
        itemWidgets.clear()
    }

    private fun drawBackground(ctx: DrawContext) {
        ctx.rect(0f, 0f, LOGICAL_W, LOGICAL_H, COL_GUI_BG, GUI_RADIUS)
    }

    private fun drawSidebar(ctx: DrawContext) {
        ctx.line(SEPARATOR_X, 12f, SEPARATOR_X, LOGICAL_H - 12f, 1f, COL_SEPARATOR)

        PVState.playerData?.name?.let { name ->
            val tw = ctx.textWidth(name, 11f)
            ctx.text(name, BTN_X + (BTN_W - tw) / 2f, PADDING - 2f, 11f, COL_TEXT_SELECT)
        }

        PVState.pages.forEachIndexed { i, page ->
            val by         = buttonLogicalY(i)
            val isSelected = page == PVState.currentPage
            val isHovered  = ctx.isHovered(mouseX, mouseY, BTN_X, by, BTN_W, BTN_H)

            val bgColor = when {
                isSelected -> COL_BTN_SEL
                isHovered  -> COL_BTN_HOVER
                else       -> COL_BTN_NORMAL
            }
            ctx.rect(BTN_X, by, BTN_W, BTN_H, bgColor, BTN_RADIUS)

            val textColor = if (isSelected || isHovered) COL_TEXT_SELECT else COL_TEXT_NORMAL
            val tw = ctx.textWidth(page, TEXT_SIZE)
            ctx.text(page, BTN_X + (BTN_W - tw) / 2f, by + (BTN_H - TEXT_SIZE) / 2f - 1f, TEXT_SIZE, textColor)
        }
    }

    private fun drawMainArea(ctx: DrawContext) {
        if (PVState.playerData == null) {
            val tw = ctx.textWidth(PVState.loadText, TEXT_SIZE)
            ctx.text(
                PVState.loadText,
                MAIN_X + (MAIN_W - tw) / 2f,
                MAIN_Y + (MAIN_H - TEXT_SIZE) / 2f,
                TEXT_SIZE,
                COL_TEXT_LOAD
            )
            return
        }

        ctx.pushScissor(MAIN_X, MAIN_Y, MAIN_W, MAIN_H)
        when (PVState.currentPage) {
            "Overview"  -> OverviewPage.draw(ctx, MAIN_X, MAIN_Y, MAIN_W, MAIN_H, mouseX, mouseY)
            "Profile"   -> ProfilePage.draw(ctx, MAIN_X, MAIN_Y, MAIN_W, MAIN_H, mouseX, mouseY)
            "Dungeons"  -> DungeonsPage.draw(ctx, MAIN_X, MAIN_Y, MAIN_W, MAIN_H, mouseX, mouseY)
            "Inventory" -> InventoryPage.draw(ctx, MAIN_X, MAIN_Y, MAIN_W, MAIN_H, mouseX, mouseY)
            "Pets"      -> PetsPage.draw(ctx, MAIN_X, MAIN_Y, MAIN_W, MAIN_H, mouseX, mouseY)
        }
        ctx.popScissor()
    }

    private fun recalcScale() {
        val dpr      = NVGRenderer.devicePixelRatio()
        val nvgW     = mc.window.width / dpr
        val nvgH     = mc.window.height / dpr
        val coverage = 0.8f * ProfileViewerModule.scale.toFloat()
        scale        = min(nvgW * coverage / LOGICAL_W, nvgH * coverage / LOGICAL_H)
        scale        = (scale * 2).toInt() / 2f
        originX      = ((nvgW - LOGICAL_W * scale) / 2f).toInt().toFloat()
        originY      = ((nvgH - LOGICAL_H * scale) / 2f).toInt().toFloat()
    }

    private fun buttonLogicalY(index: Int) = PADDING + index * (BTN_H + BTN_SPACING)

    private fun currentPageHandler(): PageHandler? = when (PVState.currentPage) {
        "Overview"  -> OverviewPage
        "Profile"   -> ProfilePage
        "Dungeons"  -> DungeonsPage
        "Inventory" -> InventoryPage
        "Pets"      -> PetsPage
        else        -> null
    }
}

interface PageHandler {
    fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double)
    fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {}
    fun onScroll(delta: Double) {}
}