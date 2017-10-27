package com.crypticmission.crdt.set

import com.crypticmission.crdt.randomClientId


data class TwoPhasePayload<E>(val adds: GSet<E>, val removes: GSet<E>) {
    companion object {
        fun <E> new() = TwoPhasePayload<E>(adds = GSet<E>(), removes = GSet<E>())
        fun <E> fromSet(set: Set<E>) = TwoPhasePayload<E>(GSet<E>(payload = set.toMutableSet()), GSet<E>())
    }
    fun clear() {
        adds.clear()
        removes.clear()
    }
}

interface TwoPhaseSet<E> : CrdtBaseSet<TwoPhaseSet<E>, TwoPhasePayload<E>, E> {
    abstract val valueCalculationCount: Int

    companion object {
        fun <E> fromSet(clientId: String = randomClientId(), set: Set<E>) = new(
                clientId = clientId,
                payload = TwoPhasePayload.fromSet(set)
        )

        fun <E> new(
                clientId: String = randomClientId(),
                payload: TwoPhasePayload<E> = TwoPhasePayload.new()
        ) : TwoPhaseSet<E> = CachedTwoPhaseSet<E>(UncachedTwoPhaseSet(clientId = clientId, payload = payload))

        fun <E> copy(from: TwoPhaseSet<E>) : TwoPhaseSet<E> =
                CachedTwoPhaseSet<E>(UncachedTwoPhaseSet(from.clientId, from.payload))
    }
}

data class CachedTwoPhaseSet<E>(val delegate: TwoPhaseSet<E>) : TwoPhaseSet<E> by delegate {
    var cachedValue = newCache()
    var cacheInvalid = false;

    override var valueCalculationCount = delegate.valueCalculationCount
        get() = delegate.valueCalculationCount

    override fun value(): MutableSet<E> {
        if (cacheInvalid) {
            cachedValue = newCache()
            cacheInvalid = false
        }
        return cachedValue
    }

    private fun invalidateCache() : Unit { cacheInvalid = true }
    private fun newCache() = delegate.value().toMutableSet()

    override fun merge(clientId: String, other: TwoPhaseSet<E>): TwoPhaseSet<E> =
            CachedTwoPhaseSet<E>(this.merge(clientId, other))

    override fun add(element: E): Boolean {
        val added = delegate.add(element)
        invalidateCache()
        return added
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val removed = delegate.addAll(elements)
        invalidateCache()
        return removed
    }

    override fun clear() {
        delegate.clear()
        invalidateCache()
    }

    override fun remove(element: E): Boolean {
        val removed = delegate.remove(element)
        invalidateCache()
        return removed
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val removed = delegate.removeAll(elements)
        invalidateCache()
        return removed
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        val retained = delegate.retainAll(elements)
        invalidateCache()
        return retained
    }
}

data class UncachedTwoPhaseSet<E>(
        override val clientId: String = randomClientId(),
        override val payload: TwoPhasePayload<E> = TwoPhasePayload.new()
) : TwoPhaseSet<E> {

    private var _valueCalculationCount = 0

    override val valueCalculationCount: Int
        get() = _valueCalculationCount

    override fun value(): MutableSet<E> {
        _valueCalculationCount++
//        println("**** calculating value() #${valueCalculationCount}")
//        println("\t ${Exception().stackTrace.filter { it.toString().contains("CachedTwoPhaseSet")}.joinToString("\n\t")}")
        return (payload.adds.value() - payload.removes.value()).toMutableSet()
    }

    override fun merge(clientId: String, other: TwoPhaseSet<E>): TwoPhaseSet<E> = UncachedTwoPhaseSet<E>(
            clientId = clientId,
            payload = TwoPhasePayload(
                    adds = this.payload.adds.merge(other.payload.adds),
                    removes = this.payload.removes.merge(other.payload.removes)
            )
    )

    override fun add(element: E): Boolean {
        val added = !contains(element) && !payload.removes.contains(element) // won
        payload.adds.add(element)
        return added
    }

    override fun addAll(elements: Collection<E>): Boolean = elements
            .map { element -> add(element) }
            .all { added -> added }

    override fun clear() = payload.clear()

    override fun iterator(): MutableIterator<E> = value().iterator()

    override fun remove(element: E): Boolean {
        val exists = contains(element)
        payload.removes.add(element)
        return exists
    }

    override fun removeAll(elements: Collection<E>): Boolean = elements
            .map { element -> remove(element) }
            .all { removed -> removed }

    override fun retainAll(elements: Collection<E>): Boolean {
        val allRetained = elements.all { contains(it) }
        removeAll(this.value() - elements)
        return allRetained
    }

    override val size: Int
        get() = this.value().size

    override fun contains(element: E): Boolean = payload.adds.contains(element) && !payload.removes.contains(element)

    fun containsNone(elements: Collection<E>): Boolean = elements
            .map { element -> !contains(element) }
            .all { contained -> contained }

    override fun containsAll(elements: Collection<E>): Boolean = elements
            .map { element -> contains(element) }
            .all { contained -> contained }

    override fun isEmpty(): Boolean = value().isEmpty()
}

