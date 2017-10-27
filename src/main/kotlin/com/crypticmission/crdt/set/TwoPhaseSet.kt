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

class CachedTwoPhaseSet<E>(_delegate: TwoPhaseSet<E>) : CachedCrdtBaseSet<TwoPhaseSet<E>, TwoPhasePayload<E>, E>(_delegate), TwoPhaseSet<E> {
    override fun merge(clientId: String, other: TwoPhaseSet<E>): TwoPhaseSet<E> =
        CachedTwoPhaseSet<E>(delegate.merge(clientId, other))
}

data class UncachedTwoPhaseSet<E>(
        override val clientId: String = randomClientId(),
        override val payload: TwoPhasePayload<E> = TwoPhasePayload.new()
) : TwoPhaseSet<E> {

    override fun value(): MutableSet<E> {
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
            .any { added -> added }

    override fun clear() = payload.clear()

    override fun iterator(): MutableIterator<E> = value().iterator()

    override fun remove(element: E): Boolean {
        val exists = contains(element)
        payload.removes.add(element)
        return exists
    }

    override fun removeAll(elements: Collection<E>): Boolean = elements
            .map { element -> remove(element) }
            .any { removed -> removed }

    override fun retainAll(elements: Collection<E>): Boolean = removeAll(this.value() - elements)

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

