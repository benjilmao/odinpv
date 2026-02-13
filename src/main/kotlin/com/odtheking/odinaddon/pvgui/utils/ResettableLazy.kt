package com.odtheking.odinaddon.pvgui.utils

import kotlin.reflect.KProperty

fun <T> resettableLazy(initializer: () -> T) = ResettableLazy.create(initializer)

class ResettableLazy<out T>(private val initializer: () -> T) {
    private var _value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        _value ?: initializer().also { _value = it }

    fun reset() { _value = null }

    companion object {
        private val allLazies = mutableListOf<ResettableLazy<*>>()

        fun <T> create(initializer: () -> T): ResettableLazy<T> =
            ResettableLazy(initializer).also { allLazies.add(it) }

        fun resetAll() = allLazies.forEach { it.reset() }
    }
}