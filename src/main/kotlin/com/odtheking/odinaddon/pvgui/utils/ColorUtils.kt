package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors

/**
 * Utilities for handling Minecraft color codes with NVG.
 * Wraps Odin's Color class for consistency.
 */
object ColorUtils {

    /**
     * Minecraft color code map to Odin Colors.
     */
    private val colorMap = mapOf(
        '0' to Colors.BLACK,
        '1' to Colors.MINECRAFT_DARK_BLUE,
        '2' to Colors.MINECRAFT_DARK_GREEN,
        '3' to Colors.MINECRAFT_DARK_AQUA,
        '4' to Colors.MINECRAFT_DARK_RED,
        '5' to Colors.MINECRAFT_DARK_PURPLE,
        '6' to Colors.MINECRAFT_GOLD,
        '7' to Colors.MINECRAFT_GRAY,
        '8' to Colors.MINECRAFT_DARK_GRAY,
        '9' to Colors.MINECRAFT_BLUE,
        'a' to Colors.MINECRAFT_GREEN,
        'b' to Colors.MINECRAFT_AQUA,
        'c' to Colors.MINECRAFT_RED,
        'd' to Colors.MINECRAFT_LIGHT_PURPLE,
        'e' to Colors.MINECRAFT_YELLOW,
        'f' to Colors.WHITE,
    )

    /**
     * Get Odin Color from Minecraft color code.
     */
    fun getColorFromCode(code: Char): Color {
        return colorMap[code.lowercaseChar()] ?: Colors.WHITE
    }

    /**
     * Parse a string with Minecraft color codes and return segments.
     * @return List of (text, color) pairs
     */
    fun parseColoredText(text: String, initialColor: Int = Colors.WHITE.rgba): List<Pair<String, Int>> {
        val segments = mutableListOf<Pair<String, Int>>()
        var currentColor = initialColor
        val builder = StringBuilder()

        var i = 0
        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) {
                if (builder.isNotEmpty()) {
                    segments.add(builder.toString() to currentColor)
                    builder.clear()
                }

                val code = text[i + 1]
                currentColor = colorMap[code]?.rgba ?: initialColor
                i += 2
            } else {
                builder.append(text[i])
                i++
            }
        }

        if (builder.isNotEmpty()) {
            segments.add(builder.toString() to currentColor)
        }

        return segments
    }

    /**
     * Strip all color codes from text.
     */
    fun stripColorCodes(text: String): String {
        return text.replace(Regex("§[0-9a-fk-or]"), "")
    }
}