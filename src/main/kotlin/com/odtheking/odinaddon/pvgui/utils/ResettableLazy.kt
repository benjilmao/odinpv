package com.odtheking.odinaddon.pvgui.utils

import kotlin.reflect.KProperty

fun <T> resettableLazy(initializer: () -> T): ResettableLazy<T> =
    ResettableLazy.create(initializer)

class ResettableLazy<out T>(private val initializer: () -> T) {

    private var _value: T? = null

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        _value ?: initializer().also { _value = it }

    fun reset() {
        _value = null
    }

    fun peek(): T? = _value

    companion object {
        private val allLazies = mutableListOf<ResettableLazy<*>>()

        fun <T> create(initializer: () -> T): ResettableLazy<T> =
            ResettableLazy(initializer).also { allLazies.add(it) }

        fun <T> silent(initializer: () -> T): ResettableLazy<T> =
            ResettableLazy(initializer)

        fun resetAll() = allLazies.forEach { it.reset() }
    }
}