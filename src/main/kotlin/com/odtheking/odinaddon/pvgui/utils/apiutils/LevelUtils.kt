package com.odtheking.odinaddon.pvgui.utils.apiutils

import com.odtheking.odinaddon.pvgui.utils.Utils.without

object LevelUtils {

    // ========== Existing dungeon functions ==========
    private fun getLevelWithProgress(experience: Double, values: Array<Long>, slope: Long = 0): Double {
        var xp = experience
        var level = 0
        val maxLevelExperience = values.last()

        for (i in values.indices) {
            val toRemove = values[i]

            if (xp < toRemove) {
                val progress = xp / toRemove
                return level + progress
            }

            xp -= toRemove
            level++
        }

        if (xp > 0 && slope <= 0) {
            level += (xp / maxLevelExperience).toInt()
            return level + (xp % maxLevelExperience / maxLevelExperience)
        } else {
            var reqSlope = slope
            var requiredXp = maxLevelExperience.toDouble() + reqSlope

            while (xp > requiredXp) {
                level++
                xp -= requiredXp
                requiredXp += reqSlope
                if (level % 10 == 0) reqSlope *= 2
            }

            if (xp < requiredXp) return level + (xp / requiredXp)
        }

        return level.toDouble()
    }

    private val dungeonsLevels: Array<Long> = arrayOf(
        50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
        1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000,
        27600, 38000, 52500, 71500, 97000, 132000, 180000,
        243000, 328000, 445000, 600000, 800000, 1065000,
        1410000, 1900000, 2500000, 3300000, 4300000, 5600000,
        7200000, 9200000, 12000000, 15000000, 19000000,
        24000000, 30000000, 38000000, 48000000, 60000000,
        75000000, 93000000, 116250000, 200000000
    )

    inline val HypixelData.DungeonsData.classAverage: Double get() =
        if (classes.isEmpty()) 0.0 else classes.values.sumOf { it.classLevel } / classes.size

    val HypixelData.DungeonTypes.cataLevel: Double get() =
        getLevelWithProgress(catacombs.experience, dungeonsLevels)

    val HypixelData.ClassData.classLevel: Double get() =
        getLevelWithProgress(experience, dungeonsLevels)

    // ========== Skill functions (top‑level, not extensions) ==========
    fun skillAverage(playerData: HypixelData.PlayerData): Double =
        playerData.experience.without("SKILL_SOCIAL", "SKILL_DUNGEONEERING").let { skills ->
            skills.entries.sumOf { (key, exp) ->
                val name = key.lowercase().substringAfter("skill_")
                getSkillLevel(name, exp)
            } / skills.size
        }

    fun cappedSkillAverage(playerData: HypixelData.PlayerData): Double =
        playerData.experience.without("SKILL_SOCIAL", "SKILL_DUNGEONEERING").let { skills ->
            skills.entries.sumOf { (key, exp) ->
                val name = key.lowercase().substringAfter("skill_")
                getSkillLevel(name, exp).coerceAtMost(getSkillCap(name).toDouble())
            } / skills.size
        }

    fun getSkillLevel(skill: String, exp: Double): Double =
        getLevelWithProgress(exp, getSkillLevels(skill), 600000)

    fun getSkillCap(skill: String): Int = when (skill) {
        "taming"       -> 60
        "mining"       -> 60
        "foraging"     -> 54
        "enchanting"   -> 60
        "carpentry"    -> 50
        "farming"      -> 60
        "combat"       -> 60
        "fishing"      -> 50
        "alchemy"      -> 50
        "runecrafting" -> 25
        "social"       -> 25
        else           -> -1
    }

    fun getSkillLevels(skill: String): Array<Long> = when (skill) {
        "runecrafting" -> runeCraftingLevels
        "social"       -> socialLevels
        else           -> skillLevels
    }

    fun getSkillColorCode(skill: String): String = when (skill) {
        "taming"       -> "d"
        "mining"       -> "7"
        "foraging"     -> "2"
        "enchanting"   -> "5"
        "carpentry"    -> "e"
        "farming"      -> "6"
        "combat"       -> "c"
        "fishing"      -> "b"
        "alchemy"      -> "d"
        "runecrafting" -> "3"
        "social"       -> "a"
        else           -> "f"
    }

    private val skillLevels: Array<Long> = arrayOf(
        50, 125, 200, 300, 500, 750, 1000, 1500,
        2000, 3500, 5000, 7500, 10000, 15000, 20000,
        30000, 50000, 75000, 100000, 200000, 300000,
        400000, 500000, 600000, 700000, 800000, 900000,
        1000000, 1100000, 1200000, 1300000, 1400000,
        1500000, 1600000, 1700000, 1800000, 1900000,
        2000000, 2100000, 2200000, 2300000, 2400000,
        2500000, 2600000, 2750000, 2900000, 3100000,
        3400000, 3700000, 4000000, 4300000, 4600000,
        4900000, 5200000, 5500000, 5800000, 6100000,
        6400000, 6700000, 7000000
    )

    private val socialLevels: Array<Long> = arrayOf(
        50, 100, 150, 250, 500, 750, 1000, 1250, 1500,
        2000, 2500, 3000, 3750, 4500, 6000, 8000, 10000,
        12500, 15000, 20000, 25000, 30000, 35000, 40000,
        50000
    )

    private val runeCraftingLevels: Array<Long> = arrayOf(
        50, 100, 125, 160, 200, 250, 315, 400, 500, 625,
        785, 1000, 1250, 1565, 2000, 2500, 3125, 4000,
        5000, 6250, 7850, 9800, 12250, 15300, 19100
    )

    // ========== Slayer functions ==========
    fun getSlayerSkillLevel(exp: Double, slayer: String): Double =
        (if (slayer != "vampire") getLevelWithProgress(exp, slayerLevels) else getLevelWithProgress(exp, vampireLevels))
            .coerceAtMost(getSlayerCap(slayer).toDouble())

    fun getSlayerColorCode(slayer: String): String = when (slayer) {
        "wolf"      -> "f"
        "zombie"    -> "a"
        "enderman"  -> "5"
        "vampire"   -> "c"
        "blaze"     -> "6"
        "spider"    -> "8"
        else        -> "f"
    }

    fun getSlayerCap(slayer: String): Int = if (slayer == "vampire") 5 else 9

    private val slayerLevels: Array<Long> = arrayOf(5, 10, 185, 800, 4000, 15000, 80000, 300000, 600000)
    private val vampireLevels: Array<Long> = arrayOf(20, 55, 165, 600, 1560)
}