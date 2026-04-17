package com.odtheking.odinaddon.pvgui

import kotlin.math.floor

const val TOTAL_W = 1000f
const val TOTAL_H = 560f
const val MAIN_START = 216f
const val PAD = 10f
const val MAIN_W = TOTAL_W - (MAIN_START + PAD)
const val MAIN_H = TOTAL_H - 2f * PAD

const val CONTENT_X = MAIN_START
const val CONTENT_Y = PAD

const val QUAD_W = (MAIN_W / 2f) - (PAD / 2f)
const val SIDEBAR_BTN_W = MAIN_START - 2f * PAD
const val SIDEBAR_CENTER_X = MAIN_START / 2f

val SIDEBAR_BTN_H = (0.9f * MAIN_H - PAD * 6f) / 5f
val SIDEBAR_INFO_Y = PAD + (SIDEBAR_BTN_H + PAD) * 5f
val SIDEBAR_INFO_H = TOTAL_H - SIDEBAR_INFO_Y - PAD
val SIDEBAR_BTNS_AREA_H = SIDEBAR_INFO_Y - PAD - PAD

val INV_TAB_H = ((TOTAL_H - PAD * 7f) * 0.9f) / 6f
val INV_SEP_Y = PAD + INV_TAB_H
val INV_START_Y = INV_SEP_Y + PAD + 1f
val INV_BTN_H = (MAIN_W - PAD * 16f) / 18f
val INV_CENTER_Y = (INV_START_Y + INV_BTN_H + PAD) + (MAIN_H - (INV_START_Y + INV_BTN_H)) / 2f

val INV_TALI_SEP_X = floor(MAIN_START + MAIN_W * 0.38f)
val INV_TALI_GRID_X = INV_TALI_SEP_X + PAD + 1f
val INV_TALI_GRID_W = MAIN_W - (INV_TALI_SEP_X - MAIN_START) - PAD