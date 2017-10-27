package com.crypticmission.crdt.set

import com.crypticmission.crdt.randomClientId

abstract class CachedCrdtBaseSet<S: CrdtBaseSet<S, P, E>, P, E>(val delegate: CrdtBaseSet<S, P, E>) : CrdtBaseSet<S, P, E> by delegate {
    var cachedValue = newCache()
    var cacheInvalid = false;

    var _valueCalculationCount = 0
    var valueCalculationCount : Int = _valueCalculationCount
        get() = _valueCalculationCount

    abstract override fun merge(clientId: String, other: S): S

    override fun merge(other: S): S = this.merge(randomClientId(), other)

    override fun value(): MutableSet<E> {
        if (cacheInvalid) {
            cachedValue = newCache()
            cacheInvalid = false
        }
        return cachedValue
    }

    private fun invalidateCache() : Unit { cacheInvalid = true }

    private fun newCache() : MutableSet<E> {
        _valueCalculationCount++
        return delegate.value().toMutableSet()
    }


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