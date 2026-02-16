package com.odtheking.odinaddon.pvgui.core

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.ui.animations.EaseOutAnimation
import com.odtheking.odinaddon.pvgui.utils.NVGSpecialRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.pages.*
import com.odtheking.odinaddon.pvgui.utils.ResettableLazy
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import com.odtheking.odinaddon.pvgui.utils.apiutils.RequestUtils
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

object PVGui : Screen(Component.literal("Profile Viewer")) {

    private val openAnim = EaseOutAnimation(400)
    private var isRendering = false

    var playerData: HypixelData.PlayerInfo? = null
    var profileName: String? = null
    var loadText = "Loading..."

    val dummyPlayer = HypixelData.PlayerInfo(
        HypixelData.ProfilesData(),
        "",
        "Loading..."
    )

    private fun getTranslation(scale: Float): Pair<Int, Int> {
        val window = mc.window
        val translateX = (window.width - (PageData.totalWidth * scale)).toInt() / 2
        val translateY = (window.height - (PageData.totalHeight * scale)).toInt() / 2
        return translateX to translateY
    }

    fun loadPlayer(playerName: String) = scope.launch {
        loadText = "Loading $playerName..."
        RequestUtils.getProfile(playerName).fold(
            onSuccess = { data ->
                if (data.profileData.profiles.isEmpty()) {
                    loadText = "No profiles found for ${data.name}."
                    return@launch
                }
                playerData = data
                profileName = data.profileData.profiles.find { it.selected }?.cuteName

                ResettableLazy.resetAll()

                mc.execute {
                    Overview.setPlayer(data)
                    Dungeons.setPlayer(data)
                    Pets.setPlayer(data)
                    Profile.setPlayer(data)
                    Inventory.setPlayer(data)
                    updateProfile(profileName)
                }
            },
            onFailure = { error ->
                modMessage(error.message ?: "Unknown error")
                loadText = "Failed to grab profile data."
            }
        )
    }

    fun updateProfile(profile: String?) {
        playerData?.let { data ->
            val selectedProfile = data.profileData.profiles.find {
                profile?.let { p -> it.cuteName == p } ?: it.selected
            }
            profileName = selectedProfile?.cuteName
        }
    }

    override fun init() {
        super.init()
        openAnim.start()
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        PageData.scale = ProfileViewerModule.scale.toFloat()

        if (isRendering) return
        isRendering = true
        try {
            super.render(context, mouseX, mouseY, deltaTicks)

        val window = mc.window
        val scale = PageData.scale

        val (translateX, translateY) = getTranslation(scale)
        PageData.offsetX = translateX
        PageData.offsetY = translateY

        NVGSpecialRenderer.draw(context, 0, 0, window.width, window.height) {
            NVGRenderer.translate(translateX.toFloat(), translateY.toFloat())
            NVGRenderer.scale(scale, scale)

            val useAnim = ProfileViewerModule.animations
            val animOffset = if (useAnim) openAnim.get(-10f, 0f, !openAnim.isAnimating()) else 0f
            if (useAnim) {
                NVGRenderer.translate(0f, animOffset)
                if (openAnim.isAnimating()) {
                    NVGRenderer.globalAlpha(openAnim.get(0f, 1f, false))
                }
            }

            val rawMouseX = mc.mouseHandler.xpos().toInt()
            val rawMouseY = mc.mouseHandler.ypos().toInt()
            val relativeMouseX = ((rawMouseX - translateX) / scale).toInt()
            val relativeMouseY = ((rawMouseY - translateY) / scale).toInt()

            PageHandler.preDraw(context, relativeMouseX, relativeMouseY)

            if (useAnim) {
                if (openAnim.isAnimating()) {
                    NVGRenderer.globalAlpha(1f)
                }
                NVGRenderer.translate(0f, -animOffset)
            }

            NVGRenderer.scale(1f / scale, 1f / scale)
            NVGRenderer.translate(-translateX.toFloat(), -translateY.toFloat())
            }
        } finally {
            isRendering = false
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val rawX = mc.mouseHandler.xpos().toInt()
        val rawY = mc.mouseHandler.ypos().toInt()
        val scale = PageData.scale
        val (translateX, translateY) = getTranslation(scale)
        val relativeX = ((rawX - translateX) / scale).toInt()
        val relativeY = ((rawY - translateY) / scale).toInt()

        PageHandler.handleClick(relativeX, relativeY, mouseButtonEvent.button())
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun onClose() {
        loadText = "Loading..."
        playerData = null
        profileName = null
        PageHandler.reset()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}