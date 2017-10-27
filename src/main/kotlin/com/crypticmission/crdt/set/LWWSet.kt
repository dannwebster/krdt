package com.crypticmission.crdt.set

import com.crypticmission.crdt.CrdtBase
import com.crypticmission.crdt.randomClientId
import java.time.Clock
import java.time.Instant

typealias TimestampedSet<E> = MutableMap<E, Instant>

enum class Bias(val keepWhen: (addTime: Instant, removeTime: Instant?) -> Boolean ) {
    TOWARDS_ADDING(keepWhen = { addTime, removeTime -> addTime >= (removeTime ?: Instant.MIN) } ),
    TOWARDS_REMOVING(keepWhen = { addTime, removeTime -> addTime > (removeTime ?: Instant.MIN) } )
}

fun <E> TimestampedSet<E>.merge(other: TimestampedSet<E>) : TimestampedSet<E> = (this.keys + other.keys)
        .map { key -> Pair(key, maxOf(this[key] ?: Instant.MIN, other[key] ?: Instant.MIN)) }
        .toMap()
        .toMutableMap()

data class ARPayload<E>(val adds: TimestampedSet<E>, val removes: TimestampedSet<E>) {
    companion object {
        fun <T> new() : ARPayload<T> = ARPayload<T>(mutableMapOf<T, Instant>(), mutableMapOf<T, Instant>())
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
) : CrdtBase<LWWSet<E>, ARPayload<E>, MutableSet<E>>, MutableSet<E> {


    override fun merge(clientId: String, other: LWWSet<E>): LWWSet<E> = LWWSet(
            bias = this.bias,
            clientId = clientId,
            payload = ARPayload(
                    adds = this.payload.adds.merge(other.payload.adds),
                    removes = this.payload.removes.merge(other.payload.removes)
            )
    )

    override fun value(): MutableSet<E> = this.payload.adds
            .filter { (key, addTime) -> val foo = bias.keepWhen(addTime, this.payload.removes[key]); println("${bias} : ${foo}"); foo }
            .map { it.key }
            .toMutableSet()

    override fun add(element: E): Boolean {
        val exists = this.value().contains(element)
        payload.adds.put(element, clock.instant())
        return !exists
    }

    override fun addAll(elements: Collection<E>): Boolean = elements.map { this.add(it) }.all { it }

    override fun clear() {
        payload.adds.clear()
        payload.removes.clear()
    }

    override fun iterator(): MutableIterator<E> = value().iterator()

    override fun remove(element: E): Boolean = with(payload) {
        val existed = adds.containsKey(element)
        removes.put(element, clock.instant())
        existed
    }

    override fun removeAll(elements: Collection<E>): Boolean = elements.map { this.remove(it) }.all { it }

    override fun retainAll(elements: Collection<E>): Boolean = removeAll( this.value() - elements )

    override val size: Int = value().size

    override fun contains(element: E): Boolean = value().contains(element)

    override fun containsAll(elements: Collection<E>): Boolean = value().containsAll(elements)

    override fun isEmpty(): Boolean = value().isEmpty()
}
