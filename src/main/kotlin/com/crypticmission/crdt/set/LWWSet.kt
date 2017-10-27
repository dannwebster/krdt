package com.crypticmission.crdt.set

import com.crypticmission.crdt.CrdtBase
import com.crypticmission.crdt.randomClientId
import java.time.Clock
import java.time.Instant

typealias TimestampedSet<E> = MutableMap<E, Instant>

enum class Bias {
    TOWARDS_ADDING {
        override fun keepWhen(addTime: Instant, removeTime: Instant?) = addTime >= (removeTime ?: Instant.MIN)
    },

    TOWARDS_REMOVING {
        override fun keepWhen(addTime: Instant, removeTime: Instant?) = addTime > (removeTime ?: Instant.MIN)
    };

    abstract fun keepWhen(addTime: Instant, removeTime: Instant?): Boolean
}

fun <E> TimestampedSet<E>.merge(other: TimestampedSet<E>) : TimestampedSet<E> = (this.keys + other.keys)
        .map { key -> Pair(key, maxOf(this[key] ?: Instant.MIN, other[key] ?: Instant.MIN)) }
        .toMap()
        .toMutableMap()

data class ARPayload<E>(val adds: TimestampedSet<E>, val removes: TimestampedSet<E>) {
    companion object {
        fun <T> new() : ARPayload<T> = ARPayload<T>(mutableMapOf<T, Instant>(), mutableMapOf<T, Instant>())
    }

    fun clear() {
        adds.clear()
        removes.clear()
    }
}
/**
 * Last Write Wins
 */
class LWWSet<E>(
        override val clientId: String = randomClientId(),
        override val payload: ARPayload<E> = ARPayload.new(),
        val clock: Clock = Clock.systemUTC(),
        val bias: Bias = Bias.TOWARDS_REMOVING
) : CrdtBaseSet<LWWSet<E>, ARPayload<E>, E> {


    override fun merge(clientId: String, other: LWWSet<E>): LWWSet<E> = LWWSet(
            bias = this.bias,
            clientId = clientId,
            payload = ARPayload(
                    adds = this.payload.adds.merge(other.payload.adds),
                    removes = this.payload.removes.merge(other.payload.removes)
            )
    )

    override fun value(): MutableSet<E> = this.payload.adds
            // TODO #1: need a cached value for value(), that is invalidated on adds or removes
            .filter { (key, addTime) -> bias.keepWhen(addTime, this.payload.removes[key]) }
            .map { it.key }
            .toMutableSet()

    override fun add(element: E): Boolean {
        // TODO #1: need a cached value for value(), that is invalidated on adds or removes
        val exists = this.value().contains(element)
        payload.adds.put(element, clock.instant())
        return !exists
    }

    override fun addAll(elements: Collection<E>): Boolean = elements
            // TODO #1: need a cached value for value(), that is invalidated on adds or removes
            // TODO #2: massively inefficient: keeps recalculating value()
            .map { element -> add(element) }
            .all { added -> added }

    override fun clear() = payload.clear()
        // TODO #1: need a cached value for value(), that is invalidated on adds or removes

    override fun iterator(): MutableIterator<E> = value().iterator()

    override fun remove(element: E): Boolean = with(payload) {
        // TODO #1: need a cached value for value(), that is invalidated on adds or removes
        val existed = adds.containsKey(element)
        removes.put(element, clock.instant())
        existed
    }

    override fun removeAll(elements: Collection<E>): Boolean = elements
            // TODO #1: need a cached value for value(), that is invalidated on adds or removes
            // TODO #2: massively inefficient: keeps recalculating value()
            .map { element -> remove(element) }
            .all { added -> added }

    // TODO #1: need a cached value for value(), that is invalidated on adds or removes
    override fun retainAll(elements: Collection<E>): Boolean = removeAll( this.value() - elements )

    override val size: Int = value().size

    override fun contains(element: E): Boolean = value().contains(element)

    override fun containsAll(elements: Collection<E>): Boolean = value().containsAll(elements)

    override fun isEmpty(): Boolean = value().isEmpty()
}
