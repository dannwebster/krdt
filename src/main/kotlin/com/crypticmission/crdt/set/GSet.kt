package com.crypticmission.crdt.set

import com.crypticmission.crdt.CrdtBase
import com.crypticmission.crdt.randomClientId
import java.util.function.Predicate

/**
 */
data class GSet<E>(
        override val clientId: String = randomClientId(),
        override val payload: MutableSet<E> = mutableSetOf<E>()
) : CrdtBaseSet<GSet<E>, Set<E>, E>, MutableSet<E> by payload {

    override fun merge(clientId: String, other: GSet<E>): GSet<E> = GSet(
            clientId = clientId,
            payload = (other.payload + this.payload).toMutableSet()
    )

    override fun value(): Set<E> = payload

    override fun remove(element: E): Boolean = growOnly()

    override fun removeAll(elements: Collection<E>): Boolean = growOnly()

    override fun removeIf(filter: Predicate<in E>): Boolean = growOnly()

    private fun growOnly() : Nothing = throw IllegalAccessException("${this.javaClass.name} is a grow-only set")
}