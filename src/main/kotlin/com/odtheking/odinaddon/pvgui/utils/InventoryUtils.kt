package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData

object InventoryUtils {
    /**
     * Fixes the first 9 items (hotbar) by moving them to the end
     */
    fun fixFirstNine(items: List<HypixelData.ItemData?>): List<HypixelData.ItemData?> {
        if (items.size < 9) return items
        val hotbar = items.take(9)
        val rest = items.drop(9)
        return rest + hotbar
    }

    /**
     * Get a subset of items for pagination
     * @param items Full list of items
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     */
    fun <T> getSubset(items: List<T>, page: Int, pageSize: Int = 36): List<T> {
        val start = page * pageSize
        val end = (start + pageSize).coerceAtMost(items.size)

        if (start >= items.size) return emptyList()

        return items.subList(start, end)
    }

    /**
     * Inserts items at specific indexes in a list
     * Used for wardrobe equipped items overlay
     */
    fun <T> insertItemsAtIndexes(base: List<T?>, insertions: List<Pair<Int, T>>): List<T?> {
        val result = base.toMutableList()
        insertions.forEach { (index, item) ->
            if (index in result.indices) {
                result[index] = item
            }
        }
        return result
    }
}