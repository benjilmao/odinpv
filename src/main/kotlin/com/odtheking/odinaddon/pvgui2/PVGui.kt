package com.odtheking.odinaddon.pvgui2

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui2.utils.HypixelData
import com.odtheking.odinaddon.pvgui2.utils.RequestUtils
import com.odtheking.odinaddon.pvgui2.pages.*
import kotlinx.coroutines.launch
import me.owdding.lib.builder.LayoutFactory
import me.owdding.lib.displays.Display
import me.owdding.lib.displays.Displays
import me.owdding.lib.displays.asButtonLeft
import me.owdding.lib.platform.drawRoundedRectangle
import me.owdding.lib.platform.screens.MeowddingScreen
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.platform.drawFilledBox
import tech.thatgravyboat.skyblockapi.utils.text.TextColor

fun Display.withRoundedBackground(color: UInt, radius: Float = 6f): Display = object : Display {
    override fun getWidth() = this@withRoundedBackground.getWidth()
    override fun getHeight() = this@withRoundedBackground.getHeight()
    override fun render(graphics: GuiGraphics) {
        graphics.drawRoundedRectangle(0, 0, getWidth(), getHeight(), color, 0x00000000U, radius, 0)
        this@withRoundedBackground.render(graphics)
    }
}

object PVGui : MeowddingScreen("Profile Viewer") {

    var playerData: HypixelData.PlayerInfo? = null
    var profileName: String? = null
    var loadText = "Loading..."

    val guiWidth get() = (mc.window.width / mc.window.guiScale * 0.85 * ProfileViewerModule.scale).toInt()
    val guiHeight get() = (mc.window.height / mc.window.guiScale * 0.85 * ProfileViewerModule.scale).toInt()
    val guiX get() = (width - guiWidth) / 2
    val guiY get() = (height - guiHeight) / 2

    val spacer get() = (guiWidth * 0.007).toInt().coerceAtLeast(1)
    private val sidebarWidth get() = (guiWidth * 0.18).toInt()
    val mainX get() = guiX + sidebarWidth + spacer * 2
    val mainY get() = guiY + spacer
    val mainWidth get() = guiWidth - sidebarWidth - spacer * 3
    val mainHeight get() = guiHeight - spacer * 2
    private val buttonHeight get() = (mainHeight - (pages.size - 1) * spacer) / pages.size
    private val buttonWidth get() = sidebarWidth - spacer * 2

    private val pages = listOf("Overview", "Profile", "Dungeons", "Inventory", "Pets")
    var currentPage = "Overview"

    fun loadPlayer(name: String) = scope.launch {
        loadText = "Loading $name..."
        playerData = null
        profileName = null
        RequestUtils.getProfile(name).fold(
            onSuccess = { data ->
                if (data.profileData.profiles.isEmpty()) {
                    loadText = "No profiles found for ${data.name}."
                    return@launch
                }
                playerData = data
                profileName = data.profileData.profiles.find { it.selected }?.cuteName
                mc.execute { init() }
            },
            onFailure = { error ->
                modMessage(error.message ?: "Unknown error")
                loadText = "Failed to load profile."
            }
        )
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (currentPage == "Pets") {
            PetsPage.scroll(scrollY)
            init()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    public override fun init() {
        super.init()
        clearWidgets()

        if (playerData == null) {
            LayoutFactory.vertical(0) {
                display(Displays.center(guiWidth, guiHeight,
                    Displays.text(loadText, shadow = true)
                ))
            }.apply {
                setPosition(guiX, guiY)
            }.visitWidgets(this::addRenderableWidget)
            return
        }

        if (currentPage != "Pets") PetsPage.reset()
        if (currentPage != "Inventory") InventoryPage.reset()

        LayoutFactory.vertical(spacer) {
            pages.forEach { page ->
                val isSelected = page == currentPage
                val button = Displays.center(buttonWidth, buttonHeight,
                    Displays.text(page, color = {
                        if (isSelected) TextColor.WHITE.toUInt() else 0xFFAAAAAA.toUInt()
                    })
                ).withRoundedBackground(
                    if (isSelected) Theme.buttonSelect else Theme.buttonBg,
                    Theme.btnRound
                ).asButtonLeft {
                    currentPage = page
                    init()
                }.also { it.withTexture(null) }
                widget(button)
            }
        }.apply {
            setPosition(guiX + spacer, guiY + spacer)
        }.visitWidgets(this::addRenderableWidget)

        when (currentPage) {
            "Overview"    -> OverviewPage.build(this) { addRenderableWidget(it) }
            "Profile"     -> ProfilePage.build(this) { addRenderableWidget(it) }
            "Dungeons"    -> DungeonsPage.build(this) { addRenderableWidget(it) }
            "Inventory"   -> InventoryPage.build(this) { addRenderableWidget(it) }
            "Pets"        -> PetsPage.build(this) { addRenderableWidget(it) }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        graphics.drawRoundedRectangle(
            guiX, guiY, guiWidth, guiHeight,
            Theme.guiBg, 0x00000000U, Theme.guiRound, 0
        )

        graphics.drawFilledBox(
            guiX + sidebarWidth + spacer, guiY + spacer,
            1, mainHeight, Theme.separator
        )

        super.render(graphics, mouseX, mouseY, partialTicks)
    }

    override fun onClose() {
        playerData = null
        profileName = null
        loadText = "Loading..."
        currentPage = "Overview"
        PetsPage.reset()
        InventoryPage.reset()
        super.onClose()
    }

    override fun isPauseScreen() = false
}