package com.odtheking.odinaddon.pvgui.utils.api

import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odin.utils.getSkyblockRarity
import com.odtheking.odin.utils.startsWithOneOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import me.owdding.dfu.item.LegacyDataFixer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.world.item.ItemStack
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.jvm.optionals.getOrNull
import kotlin.math.floor

// Based on code from OdinFabric by odtheking
// https://github.com/odtheking/OdinFabric
// Licensed under BSD-3-Clause

object HypixelData {
    private val mpRegex = Regex("§7§4☠ §cRequires §5.+§c.")

    @Serializable
    data class PlayerInfo(
        @SerialName("profileData") val profileData: ProfilesData,
        val uuid: String,
        val name: String,
    ) {
        val memberData get() = profileData.profiles.find { it.selected }?.members?.get(uuid)
    }

    @Serializable
    data class ProfilesData(
        val error: String? = null,
        val cause: String? = null,
        @SerialName("profiles")
        private val profileList: List<Profiles>? = emptyList(),
    ) {
        val profiles get() = profileList.orEmpty()

        @Transient
        val failed: String? = when {
            error != null -> error
            cause != null -> cause
            else -> null
        }
    }

    @Serializable
    data class Profiles(
        @SerialName("profile_id") val profileId: String,
        val members: Map<String, MemberData>,
        @SerialName("game_mode") val gameMode: String?,
        val banking: BankingData = BankingData(),
        @SerialName("cute_name") val cuteName: String?,
        val selected: Boolean,
    )

    @Serializable
    data class MemberData(
        val rift: RiftData = RiftData(),
        @SerialName("accessory_bag_storage")
        val accessoryBagStorage: AccessoryBagStorage = AccessoryBagStorage(),
        @SerialName("item_data")
        val miscItemData: MiscItemData = MiscItemData(),
        val currencies: CurrencyData = CurrencyData(),
        val dungeons: DungeonsData = DungeonsData(),
        @SerialName("pets_data")
        val pets: PetsData = PetsData(),
        @SerialName("player_id")
        val playerId: String,
        @SerialName("nether_island_player_data")
        val crimsonIsle: CrimsonIsle = CrimsonIsle(),
        @SerialName("player_stats")
        val playerStats: PlayerStats = PlayerStats(),
        val inventory: Inventory? = Inventory(),
        val collection: Map<String, Long> = mapOf(),
        @SerialName("player_data")
        val playerData: PlayerData = PlayerData(),
        val slayer: Slayers = Slayers(),
        val leveling: LevelingData = LevelingData(),
        val profile: ProfileData = ProfileData()
    ) {
        val magicalPower: Int by lazy {
            inventory?.bagContents?.get("talisman_bag")?.itemStacks?.mapNotNull { itemData ->
                if (itemData == null || itemData.lore.any { mpRegex.matches(it) }) return@mapNotNull null
                val mp = itemData.magicalPower + if (itemData.id == "ABICASE") {
                    floor(crimsonIsle.abiphone.activeContacts.size / 2f).toInt()
                } else 0
                val itemId = itemData.id.takeUnless { it.startsWithOneOf("PARTY_HAT", "BALLOON_HAT") } ?: "PARTY_HAT"
                itemId to mp
            }?.groupBy { it.first }?.mapValues { entry ->
                entry.value.maxBy { it.second }
            }?.values?.sumOf { it.second }?.let { it + if (rift.access.consumedPrism) 11 else 0 } ?: 0
        }

        val tunings: List<String> get() =
            accessoryBagStorage.tuning.currentTunings.map { "${it.key.replace("_", " ").capitalizeWords()}§7: ${it.value}" }

        val inventoryApi: Boolean get() = inventory?.eChestContents?.itemStacks?.isNotEmpty() == true

        val allItems: List<ItemData?> get() =
            (inventory?.invContents?.itemStacks ?: emptyList()) +
                    (inventory?.eChestContents?.itemStacks ?: emptyList()) +
                    (inventory?.backpackContents?.flatMap { it.value.itemStacks } ?: emptyList())

        val assumedMagicalPower: Int get() = magicalPower.takeUnless { it == 0 } ?: (accessoryBagStorage.tuning.currentTunings.values.sum() * 10)
    }

    @Serializable
    data class PlayerData(
        val experience: Map<String, Double> = emptyMap()
    )

    @Serializable
    data class Slayers(
        @SerialName("slayer_bosses")
        val bosses: Map<String, SlayerData> = emptyMap()
    )

    @Serializable
    data class SlayerData(
        val xp: Long = 0,
        @SerialName("claimed_levels")
        val claimed: Map<String, Boolean> = emptyMap()
    )

    @Serializable
    data class LevelingData(
        val experience: Long = 0
    )

    @Serializable
    data class ProfileData(
        @SerialName("first_join")
        val firstJoin: Long = 0,
        @SerialName("personal_bank_upgrade")
        val personalBankUpgrade: Int = 0,
        @SerialName("bank_account")
        val bankAccount: Double = 0.0,
        @SerialName("cookie_buff_active")
        val activeCookie: Boolean = false
    )

    @Serializable
    data class PlayerStats(
        val kills: Map<String, Float> = emptyMap(),
        val deaths: Map<String, Float> = emptyMap(),
    ) {
        val bloodMobKills: Int get() =
            ((kills["watcher_summon_undead"] ?: 0f) + (kills["master_watcher_summon_undead"] ?: 0f)).toInt()
    }

    @Serializable
    data class PlayerStatus(
        val success: Boolean,
        val uuid: String,
        val session: Session? = null
    )

    @Serializable
    data class Session(
        val online: Boolean,
        val gameType: String? = null,
        val mode: String? = null,
        val map: String? = null
    )

    @Serializable
    data class CrimsonIsle(
        val abiphone: Abiphone = Abiphone(),
    )

    @Serializable
    data class Abiphone(
        @SerialName("active_contacts")
        val activeContacts: List<String> = emptyList(),
    )

    @Serializable
    data class RiftData(
        val access: RiftAccess = RiftAccess(),
    )

    @Serializable
    data class RiftAccess(
        @SerialName("consumed_prism")
        val consumedPrism: Boolean = false
    )

    @Serializable
    data class PetsData(
        val pets: List<Pet> = emptyList()
    ) {
        val activePet: Pet? get() = pets.find { it.active }
    }

    @Serializable
    data class Pet(
        val uuid: String? = null,
        val uniqueId: String? = null,
        val type: String = "",
        val exp: Double = 0.0,
        val active: Boolean = false,
        val tier: String = "",
        val heldItem: String? = null,
        val candyUsed: Int = 0,
        val skin: String? = null,
    )

    @Serializable
    data class DungeonsData(
        @SerialName("dungeon_types")
        val dungeonTypes: DungeonTypes = DungeonTypes(),
        @SerialName("player_classes")
        val classes: Map<String, ClassData> = emptyMap(),
        @SerialName("selected_dungeon_class")
        val selectedClass: String? = null,
        @SerialName("daily_runs")
        val dailyRuns: DailyRunData = DailyRunData(),
        @SerialName("last_dungeon_run")
        val lastDungeonRun: String? = null,
        val secrets: Long = 0,
    ) {
        val totalRuns: Int get() =
            (1..7).sumOf { tier ->
                (dungeonTypes.catacombs.tierComps["$tier"]?.toInt() ?: 0) +
                        (dungeonTypes.mastermode.tierComps["$tier"]?.toInt() ?: 0)
            }

        val avrSecrets: Double get() = if (totalRuns > 0) secrets.toDouble() / totalRuns else 0.0
    }

    @Serializable
    data class DungeonTypes(
        val catacombs: DungeonTypeData = DungeonTypeData(),
        @SerialName("master_catacombs")
        val mastermode: DungeonTypeData = DungeonTypeData(),
    )

    @Serializable
    data class DailyRunData(
        @SerialName("current_day_stamp")
        val currentDayStamp: Long? = null,
        @SerialName("completed_runs_count")
        val completedRunsCount: Long = 0,
    )

    @Serializable
    data class ClassData(
        val experience: Double = 0.0
    )

    @Serializable
    data class DungeonTypeData(
        @SerialName("times_played")
        val timesPlayed: Map<String, Double>? = null,
        val experience: Double = 0.0,
        @SerialName("tier_completions")
        val tierComps: Map<String, Float> = emptyMap(),
        @SerialName("milestone_completions")
        val milestoneComps: Map<String, Float> = emptyMap(),
        @SerialName("fastest_time")
        val fastestTimes: Map<String, Float> = emptyMap(),
        @SerialName("best_score")
        val bestScore: Map<String, Float> = emptyMap(),
        @SerialName("mobs_killed")
        val mobsKilled: Map<String, Float> = emptyMap(),
        @SerialName("most_mobs_killed")
        val mostMobsKilled: Map<String, Float> = emptyMap(),
        @SerialName("most_damage_berserk")
        val mostDamageBers: Map<String, Double> = emptyMap(),
        @SerialName("most_healing")
        val mostHealing: Map<String, Double> = emptyMap(),
        @SerialName("watcher_kills")
        val watcherKills: Map<String, Float> = emptyMap(),
        @SerialName("highest_tier_completed")
        val highestTierComp: Int = 0,
        @SerialName("most_damage_tank")
        val mostDamageTank: Map<String, Double> = emptyMap(),
        @SerialName("most_damage_healer")
        val mostDamageHealer: Map<String, Double> = emptyMap(),
        @SerialName("fastest_time_s")
        val fastestTimeS: Map<String, Double> = emptyMap(),
        @SerialName("most_damage_mage")
        val mostDamageMage: Map<String, Double> = emptyMap(),
        @SerialName("fastest_time_s_plus")
        val fastestTimeSPlus: Map<String, Double> = emptyMap(),
        @SerialName("most_damage_Archer")
        val mostDamageArcher: Map<String, Double> = emptyMap(),
    )

    @Serializable
    data class CurrencyData(
        @SerialName("coin_purse")
        val coins: Double = 0.0,
        @SerialName("motes_purse")
        val motes: Double = 0.0,
        val essence: Map<String, EssenceData> = emptyMap(),
    )

    @Serializable
    data class EssenceData(
        val current: Long = 0,
    )

    @Serializable
    data class MiscItemData(
        val soulflow: Long = 0,
        @SerialName("favorite_arrow")
        val favoriteArrow: String? = null,
    )

    @Serializable
    data class AccessoryBagStorage(
        val tuning: TuningData = TuningData(),
        @SerialName("selected_power")
        val selectedPower: String? = null,
        @SerialName("unlocked_powers")
        val unlockedPowers: List<String> = emptyList(),
        @SerialName("bag_upgrades_purchased")
        val bagUpgrades: Int = 0,
        @SerialName("highest_magical_power")
        val highestMP: Long = 0,
    )

    @Serializable
    data class TuningData(
        @SerialName("slot_0")
        val currentTunings: Map<String, Int> = emptyMap(),
        val highestUnlockedSlot: Int = 0,
    )

    @Serializable
    data class Inventory(
        @SerialName("inv_contents")
        val invContents: InventoryContents = InventoryContents(),
        @SerialName("ender_chest_contents")
        val eChestContents: InventoryContents = InventoryContents(),
        @SerialName("backpack_icons")
        val backpackIcons: Map<String, InventoryContents> = emptyMap(),
        @SerialName("bag_contents")
        val bagContents: Map<String, InventoryContents> = emptyMap(),
        @SerialName("inv_armor")
        val invArmor: InventoryContents = InventoryContents(),
        @SerialName("equipment_contents")
        val equipment: InventoryContents = InventoryContents(),
        @SerialName("wardrobe_equipped_slot")
        val wardrobeEquipped: Int? = null,
        @SerialName("backpack_contents")
        val backpackContents: Map<String, InventoryContents> = emptyMap(),
        @SerialName("sacks_counts")
        val sacks: Map<String, Long> = emptyMap(),
        @SerialName("personal_vault_contents")
        val personalVault: InventoryContents = InventoryContents(),
        @SerialName("wardrobe_contents")
        val wardrobeContents: InventoryContents = InventoryContents()
    )

    @Serializable
    data class BankingData(
        val balance: Double = 0.0
    )

    @Serializable
    data class InventoryContents(
        val type: Int? = null,
        val data: String = ""
    ) {
        @OptIn(ExperimentalEncodingApi::class)
        val itemStacks: List<ItemData?> get() = with(data) {
            if (isEmpty()) return emptyList()
            val nbtCompound = NbtIo.readCompressed(Base64.decode(this).inputStream(), NbtAccounter.unlimitedHeap())
            val itemNBTList = nbtCompound.getList("i").getOrNull() ?: return emptyList()
            itemNBTList.indices.map { i ->
                val compound = itemNBTList.getCompound(i).getOrNull()?.takeIf { it.size() > 0 } ?: return@map null
                val innerTag = compound.get("tag")?.asCompound()?.get() ?: return@map null
                val id = innerTag.get("ExtraAttributes")?.asCompound()?.get()?.get("id")?.asString()?.get() ?: ""
                val display = innerTag.get("display")?.asCompound()?.get() ?: return@map null
                val name = display.get("Name")?.asString()?.get() ?: ""
                val lore = display.get("Lore")?.asList()?.get()?.mapNotNull { it.asString().getOrNull() } ?: emptyList()
                ItemData(name, id, lore, compound)
            }
        }
    }

    data class ItemData(
        val name: String,
        val id: String,
        val lore: List<String>,
        val nbt: CompoundTag
    ) {
        @Transient
        val asItemStack: ItemStack by lazy {
            LegacyDataFixer.fromTag(nbt) ?: ItemStack.EMPTY
        }

        @Transient
        val magicalPower: Int by lazy {
            getSkyblockRarity(lore)?.let { rarity ->
                val base = rarity.mp
                if (id == "HEGEMONY_ARTIFACT") base * 2 else base
            } ?: 0
        }
    }
}