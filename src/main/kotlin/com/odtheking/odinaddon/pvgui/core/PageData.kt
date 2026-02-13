package com.odtheking.odinaddon.pvgui.core

object PageData {
    const val totalWidth = 1000
    const val totalHeight = 560
    const val sidebarWidth = 206   // because mainStart = sidebarWidth + spacer = 216
    const val spacer = 10

    val mainX get() = sidebarWidth + spacer          // 216
    val mainWidth get() = totalWidth - mainX - spacer // 774
    val mainHeight get() = totalHeight - 2 * spacer   // 540
    val mainCenterX get() = mainX + mainWidth / 2f

    // Translation offsets (set by PVGui each frame)
    var offsetX = 0
    var offsetY = 0

    // Global scale (should come from a config module)
    var scale = 1f
}