package com.nexomc.nexo.utils

class ObservableMap<K, V>(
    private val map: MutableMap<K, V> = mutableMapOf(),
    private val onChange: MutableMap<K, V>.() -> Unit
) : MutableMap<K, V> by map {
    override fun put(key: K, value: V): V? {
        return map.put(key, value).also { onChange() }
    }

    override fun putAll(from: Map<out K, V>) {
        map.putAll(from)
        onChange()
    }

    override fun remove(key: K): V? {
        return map.remove(key).also { onChange() }
    }

    override fun clear() {
        map.clear()
        onChange()
    }
}
