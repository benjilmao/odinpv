package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil

/**
 * Data holder for one group of items in the grid, matching HC's GridItems.
 *
 * @param items       Item list (nullable slots are empty).
 * @param x           Left edge of this grid in NVG space.
 * @param centerY     Vertical centre of this grid in NVG space.
 * @param width       Total pixel width of this grid.
 * @param columns     Number of columns.
 */
data class GridItems(
    var items: List<ItemStack?>,
    val x: Float,
    val centerY: Float,
    val width: Float,
    val columns: Int,
)

/**
 * DSL builder — matches HC's itemGrid { } pattern.
 *
 * Usage:
 *   val grid = itemGrid(listOf(GridItems(...)), radius=8f, padding=5f) {
 *       colorHandler { index, stack -> ... }
 *       tooltipHandler { stack -> listOf(stack.displayName) + stack.lore }
 *   }
 *   // each frame:
 *   grid.draw(context, mouseX, mouseY)
 *   grid.enqueueItems(context, mouseX, mouseY)
 */
fun itemGrid(
    items: List<GridItems>,
    radius: Float = Theme.slotRadius,
    padding: Float = 0f,
    block: ItemGridDsl.() -> Unit = {},
): ItemGridDsl = ItemGridDsl(items, radius, padding).apply(block)

class ItemGridDsl(
    private val groups: List<GridItems>,
    private val radius: Float,
    private val padding: Float,
) {
    private var colorHandler:   (index: Int, stack: ItemStack?) -> Int =
        { _, _ -> Theme.slotBg }
    private var tooltipHandler: (ItemStack) -> List<String> =
        { listOf(it.hoverName.string) }

    fun colorHandler(block: (index: Int, stack: ItemStack?) -> Int)   { colorHandler   = block }
    fun tooltipHandler(block: (ItemStack) -> List<String>)            { tooltipHandler = block }

    fun updateItems(newItems: List<ItemStack?>, groupIndex: Int = 0) {
        groups[groupIndex].items = newItems
    }

    // ── Draw slot backgrounds ─────────────────────────────────────────────────
    fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        groups.forEach { group ->
            val itemW = (group.width - (group.columns - 1) * padding) /
                    group.columns.coerceAtLeast(1)
            val rows  = ceil(group.items.size.toDouble() / group.columns).toInt()
            val gridH = rows * itemW + (rows - 1).coerceAtLeast(0) * padding
            val originY = group.centerY - gridH / 2f

            group.items.forEachIndexed { index, stack ->
                val col = index % group.columns
                val row = index / group.columns
                val sx  = group.x + col * (itemW + padding)
                val sy  = originY + row * (itemW + padding)
                NVGRenderer.rect(sx, sy, itemW, itemW, colorHandler(index, stack), radius)
            }
        }
    }

    // ── Enqueue items for rendering ───────────────────────────────────────────
    fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        groups.forEach { group ->
            val itemW = (group.width - (group.columns - 1) * padding) /
                    group.columns.coerceAtLeast(1)
            val rows  = ceil(group.items.size.toDouble() / group.columns).toInt()
            val gridH = rows * itemW + (rows - 1).coerceAtLeast(0) * padding
            val originY = group.centerY - gridH / 2f

            group.items.forEachIndexed { index, stack ->
                if (stack != null && !stack.isEmpty) {
                    val col = index % group.columns
                    val row = index / group.columns
                    val sx  = group.x + col * (itemW + padding)
                    val sy  = originY + row * (itemW + padding)
                    RenderQueue.enqueueItem(stack, sx, sy, itemW,
                        showTooltip = true)
                }
            }
        }
    }

    // ── Click / hover detection ───────────────────────────────────────────────
    fun click(mouseX: Double, mouseY: Double): Boolean {
        groups.forEach { group ->
            val itemW = (group.width - (group.columns - 1) * padding) /
                    group.columns.coerceAtLeast(1)
            val rows  = ceil(group.items.size.toDouble() / group.columns).toInt()
            val gridH = rows * itemW + (rows - 1).coerceAtLeast(0) * padding
            val originY = group.centerY - gridH / 2f

            group.items.forEachIndexed { index, _ ->
                val col = index % group.columns
                val row = index / group.columns
                val sx  = group.x + col * (itemW + padding)
                val sy  = originY + row * (itemW + padding)
                if (mouseX >= sx && mouseX < sx + itemW &&
                    mouseY >= sy && mouseY < sy + itemW) return true
            }
        }
        return false
    }
}