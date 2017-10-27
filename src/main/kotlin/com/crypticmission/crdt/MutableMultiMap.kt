package com.crypticmission.crdt

class MutableMultiMap<K, V>(initial: Map<K, List<V>> = mapOf()) : MutableMap<K, V> {
    // mutablemap
    override fun clear() = internal.clear()

    override fun put(key: K, value: V): V? {
        internal.getOrPut(key, { -> mutableListOf<V>() }).add(value)
        return value;
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach{ (key, value) ->
            internal.getOrPut(key, { -> mutableListOf<V>() }).add(value)
        }
    }

    fun put(key: K, value: List<V>): List<V>? {
        internal.getOrPut(key, { -> mutableListOf<V>() }).addAll(value)
        return value;
    }

    fun putAllValues(from: Map<out K, List<V>>) {
        from.forEach{ (key, value) ->
            internal.getOrPut(key, { -> mutableListOf<V>() }).addAll(value)
        }
    }

    data class Entry<K, V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            this.value = newValue;
            return value
        }
    }

    private val internal: MutableMap<K, MutableList<V>> = initial
            .mapValues { (_, list) -> list.toMutableList() }.toMutableMap()


    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = internal
            .entries
            .flatMap { (key, list) -> list.map { value -> Entry(key, value) } }
            .toMutableSet()

    override val keys: MutableSet<K> get() = internal.keys
    override val size: Int get() = internal.size
    override val values: MutableCollection<V> = internal.values.flatten().toMutableList()
    override fun containsKey(key: K): Boolean = internal.containsKey(key)
    override fun containsValue(value: V): Boolean = internal.values.flatten().contains(value)
    override fun isEmpty(): Boolean = internal.isEmpty()

    fun getValues(key: K): List<V>? = internal[key]
    override fun get(key: K): V? {
        TODO("not implemented -- return semantics incorrect")
    }

    override fun remove(key: K): V? {
        TODO("not implemented -- return semantics incorrect")
    }
}