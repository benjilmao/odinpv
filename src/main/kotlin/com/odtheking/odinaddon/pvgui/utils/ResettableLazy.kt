package com.odtheking.odinaddon.pvgui.utils

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

fun <T> resettableLazy(initializer: () -> T): ResettableLazy<T> = ResettableLazy.create(initializer)

class ResettableLazy<out T>(private val initializer: () -> T) {
    private var value: T? = null

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value ?: initializer().also { value = it }

    fun reset() { value = null }

    companion object {
        private val all = mutableListOf<WeakReference<ResettableLazy<*>>>()

        fun <T> create(initializer: () -> T): ResettableLazy<T> =
            ResettableLazy(initializer).also { all.add(WeakReference(it)) }

        fun resetAll() = all.forEach { it.get()?.reset() }
    }
}