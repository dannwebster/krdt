package com.crypticmission.crdt.set

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

    companion object {
      val DEFAULT = TOWARDS_REMOVING
    }

    abstract fun keepWhen(addTime: Instant, removeTime: Instant?): Boolean
}

fun <E> TimestampedSet<E>.merge(other: TimestampedSet<E>) : TimestampedSet<E> = (this.keys + other.keys)
        .map { key -> Pair(key, maxOf(this[key] ?: Instant.MIN, other[key] ?: Instant.MIN)) }
        .toMap()
        .toMutableMap()

fun <E> toTimestamptedSet(elements: Set<E>, clock: Clock) : TimestampedSet<E> {
    val set: TimestampedSet<E> = mutableMapOf()
    elements.forEach { element -> set[element] = clock.instant() }
    return set
}

data class LWWPayload<E>(val adds: TimestampedSet<E>, val removes: TimestampedSet<E>) {
    companion object {
        fun <T> new() : LWWPayload<T> = LWWPayload<T>(mutableMapOf<T, Instant>(), mutableMapOf<T, Instant>())
        fun <T> fromSet(set: Set<T>, clock: Clock = Clock.systemUTC()) =
                LWWPayload<T>(toTimestamptedSet(set, clock), mutableMapOf<T, Instant>())
    }

    fun clear() {
        adds.clear()
        removes.clear()
    }
}

interface LWWSet<E> : CrdtBaseSet<LWWSet<E>, LWWPayload<E>, E> {

    companion object {
        fun <E> fromSet(
                    clientId: String = randomClientId(),
                    set: Set<E>,
                    clock: Clock = Clock.systemUTC(),
                    bias: Bias = Bias.DEFAULT
        ) = new(
                clientId = clientId,
                payload = LWWPayload.fromSet(set, clock),
                clock = clock,
                bias = bias
        )

        fun <E> new(
                clientId: String = randomClientId(),
                payload: LWWPayload<E> = LWWPayload.new(),
                clock: Clock = Clock.systemUTC(),
                bias: Bias = Bias.DEFAULT
        ) : LWWSet<E> = CachedLwwSet<E>(UncachedLwwSet(
                clientId = clientId,
                payload = payload,
                clock = clock,
                bias = bias
        ))
    }
}

class CachedLwwSet<E>(_delegate: LWWSet<E>) : CachedCrdtBaseSet<LWWSet<E>, LWWPayload<E>, E>(_delegate), LWWSet<E> {
    override fun merge(clientId: String, other: LWWSet<E>): LWWSet<E> = CachedLwwSet(delegate.merge(other))
}

/**
 * Last Write Wins
 */
class UncachedLwwSet<E>(
        override val clientId: String = randomClientId(),
        override val payload: LWWPayload<E> = LWWPayload.new(),
        val clock: Clock = Clock.systemUTC(),
        val bias: Bias = Bias.DEFAULT
) : CrdtBaseSet<LWWSet<E>, LWWPayload<E>, E>, LWWSet<E> {

    override fun merge(clientId: String, other: LWWSet<E>): LWWSet<E> = UncachedLwwSet(
            bias = this.bias,
            clientId = clientId,
            payload = LWWPayload(
                    adds = this.payload.adds.merge(other.payload.adds),
                    removes = this.payload.removes.merge(other.payload.removes)
            )
    )

    override fun value(): MutableSet<E> {
        return this.payload.adds
                .filter { (key, addTime) -> bias.keepWhen(addTime, this.payload.removes[key]) }
                .map { it.key }
                .toMutableSet()
    }

    override fun add(element: E): Boolean {
        val exists = contains(element)
        payload.adds.put(element, clock.instant())
        return !exists
    }

    override fun addAll(elements: Collection<E>): Boolean = elements
            .map { element -> add(element) }
            .any { added -> added }

    override fun clear() = payload.clear()

    override fun iterator(): MutableIterator<E> = value().iterator()

    override fun remove(element: E): Boolean = with(payload) {
        val existed = contains(element)
        removes.put(element, clock.instant())
        existed
    }

    override fun removeAll(elements: Collection<E>): Boolean = elements
            .map { element -> remove(element) }
            .any { added -> added }

    override fun retainAll(elements: Collection<E>): Boolean = removeAll(this.value() - elements)

    override val size: Int = value().size

    override fun contains(element: E): Boolean = addedAndNotRemoved(element) || addedMoreRecentlyThanRemoved(element)

    private fun addedMoreRecentlyThanRemoved(element: E) = with(payload) {
        adds.containsKey(element) &&
                removes.containsKey(element) &&
                bias.keepWhen(adds.getOrDefault(element, Instant.MIN), removes.get(element))
    }

    private fun addedAndNotRemoved(element: E) = with(payload) {
        adds.containsKey(element) && !removes.containsKey(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean = elements
            .map { element -> contains(element) }
            .all { contains -> contains }

    override fun isEmpty(): Boolean = value().isEmpty()
}
