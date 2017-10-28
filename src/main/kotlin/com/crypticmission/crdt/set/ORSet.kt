package com.crypticmission.crdt.set

import com.crypticmission.crdt.randomClientId
import java.util.*

typealias Versioner = () -> UUID
typealias VersionVector = MutableSet<UUID>
typealias VersionedSet<E> = MutableMap<E, VersionVector>

val DEFAULT_VERSIONER : Versioner = UUID::randomUUID

fun <E> VersionedSet<E>.merge(other: VersionedSet<E>) : VersionedSet<E> = versionedSet<E>().let { newSet ->
    (this.keys + other.keys).forEach { key ->
        val allVersions = (this.getOrDefault(key, EMPTY_VECTOR) + other.getOrDefault(key, EMPTY_VECTOR)).toMutableSet()
        newSet.put(key, allVersions)
    }
    return newSet
}

fun vector() : VersionVector = mutableSetOf()
fun vector(versioner: Versioner) : VersionVector = mutableSetOf(versioner())

val EMPTY_VECTOR = vector()
fun <T> versionedSet() : VersionedSet<T> = mutableMapOf<T, VersionVector>()

fun <E> toVersionedSet(elements: Set<E>, versioner: Versioner) : VersionedSet<E> {
    val set: VersionedSet<E> = mutableMapOf()
    elements.forEach { element -> set[element] = vector(versioner) }
    return set
}

data class ORPayload<E>(val adds: VersionedSet<E>, val removes: VersionedSet<E>) {
    companion object {
        fun <T> new() : ORPayload<T> = ORPayload<T>(versionedSet(), versionedSet())

        fun <T> fromSet(set: Set<T>, versioner: Versioner = DEFAULT_VERSIONER) =
                ORPayload<T>(toVersionedSet(set, versioner), versionedSet())
    }

    fun clear() {
        adds.clear()
        removes.clear()
    }
}

interface ORSet<E> : CrdtBaseSet<ORSet<E>, ORPayload<E>, E> {

    companion object {
        fun <E> fromSet(
                clientId: String = randomClientId(),
                set: Set<E>,
                versioner: Versioner = DEFAULT_VERSIONER
        ) = new(
                clientId = clientId,
                payload = ORPayload.fromSet(set, versioner),
                versioner = versioner
        )

        fun <E> new(
                clientId: String = randomClientId(),
                payload: ORPayload<E> = ORPayload.new(),
                versioner: Versioner = DEFAULT_VERSIONER
        ) = CachedOrSet<E>(UncachedOrSet(
                clientId = clientId,
                payload = payload,
                versioner = versioner
        ))
    }
}

class CachedOrSet<E>(_delegate: ORSet<E>) : CachedCrdtBaseSet<ORSet<E>, ORPayload<E>, E>(_delegate), ORSet<E> {
    override fun merge(clientId: String, other: ORSet<E>): ORSet<E> = CachedOrSet(delegate.merge(other))
}

fun <E> Set<E>.disjoint(other: Set<E>) = this.intersect(other)

/**
 * Last Write Wins
 */
class UncachedOrSet<E>(
        override val clientId: String = randomClientId(),
        override val payload: ORPayload<E> = ORPayload.new(),
        val versioner: Versioner = DEFAULT_VERSIONER
) : CrdtBaseSet<ORSet<E>, ORPayload<E>, E>, ORSet<E> {

    override fun merge(clientId: String, other: ORSet<E>): ORSet<E> = UncachedOrSet(
            clientId = clientId,
            payload = ORPayload(
                    adds = this.payload.adds.merge(other.payload.adds),
                    removes = this.payload.removes.merge(other.payload.removes)
            ),
            versioner = versioner
    )

    override fun value(): MutableSet<E> = with(payload) {
        adds
                .filter { (element, vector) -> !(vector - removes.getOrDefault(element, EMPTY_VECTOR)).isEmpty() }
                .map { (element, _) -> element}
                .toMutableSet()
    }

    override fun add(element: E): Boolean {
        val exists = contains(element)
        payload.adds.getOrPut(element, ::vector).add(versioner())
        return !exists
    }

    override fun addAll(elements: Collection<E>): Boolean = elements
            .map { element -> add(element) }
            .any { added -> added }

    override fun clear() = payload.clear()

    override fun iterator(): MutableIterator<E> = value().iterator()

    override fun remove(element: E): Boolean {
        val existed = contains(element)
        payload.removes.getOrPut(element, ::vector).addAll(payload.adds.getOrDefault(element, EMPTY_VECTOR))
        return existed
    }

    override fun removeAll(elements: Collection<E>): Boolean = elements
            .map { element -> remove(element) }
            .any { added -> added }

    override fun retainAll(elements: Collection<E>): Boolean = removeAll(this.value() - elements)

    override val size: Int = value().size

    override fun contains(element: E): Boolean = addedAndNotRemoved(element) || moreAddsThanRemoves(element)

    private fun moreAddsThanRemoves(element: E) : Boolean = with(payload) {
        !(payload.adds.getOrDefault(element, EMPTY_VECTOR) -
                payload.removes.getOrDefault(element, EMPTY_VECTOR)).isEmpty()
    }

    private fun addedAndNotRemoved(element: E) = with(payload) {
        adds.containsKey(element) && !removes.containsKey(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean = elements
            .map { element -> contains(element) }
            .all { contains -> contains }

    override fun isEmpty(): Boolean = value().isEmpty()
}
