package com.odtheking.odinaddon.pvgui

// ── HC PageData ───────────────────────────────────────────────────────────────
const val TOTAL_W    = 1000f
const val TOTAL_H    = 560f
const val MAIN_START = 216f
const val PAD        = 10f

// HC: mainWidth  = totalWidth  - (mainStart + spacer) = 774
// HC: mainHeight = totalHeight - 2*spacer             = 540
const val MAIN_W = TOTAL_W - (MAIN_START + PAD)
const val MAIN_H = TOTAL_H - 2f * PAD

const val CONTENT_X = MAIN_START
const val CONTENT_Y = PAD

// HC: quadrantWidth = (mainWidth/2) - (spacer/2) = 382
const val QUAD_W = (MAIN_W / 2f) - (PAD / 2f)

// ── Sidebar (HC PageHandler) ──────────────────────────────────────────────────
const val SIDEBAR_BTN_W    = MAIN_START - 2f * PAD           // 196
const val SIDEBAR_CENTER_X = MAIN_START / 2f                  // 108

// HC: (0.9 * mainHeight - spacer*(count+1)) / count  where count=5
val SIDEBAR_BTN_H  = (0.9f * MAIN_H - PAD * 6f) / 5f         // ≈85.2
val SIDEBAR_INFO_Y = PAD + (SIDEBAR_BTN_H + PAD) * 5f         // ≈486
val SIDEBAR_INFO_H = TOTAL_H - SIDEBAR_INFO_Y - PAD           // ≈64

// ── Inventory page (HC Inventory.kt) ─────────────────────────────────────────
// HC: separatorLineY = spacer + ((totalHeight - spacer*(2+6-1))*0.9)/6
val INV_TAB_H   = ((TOTAL_H - PAD * 7f) * 0.9f) / 6f         // ≈73.5
val INV_SEP_Y   = PAD + INV_TAB_H                             // ≈83.5
val INV_START_Y = INV_SEP_Y + PAD + 1f                        // ≈94.5
// HC: buttonHeight = (mainWidth - spacer*16) / 18
val INV_BTN_H   = (MAIN_W - PAD * 16f) / 18f                  // ≈34.1
// HC: centerY
val INV_CENTER_Y = (INV_START_Y + INV_BTN_H + PAD) +
        (MAIN_H - (INV_START_Y + INV_BTN_H)) / 2f  // ≈344.3

// Aliases
const val GUI_W = TOTAL_W
const val GUI_H = TOTAL_H