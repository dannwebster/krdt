package com.crypticmission.crdt.set

import com.crypticmission.crdt.CrdtBase

/**
 */
interface CrdtBaseSet<C: CrdtBaseSet<C, P, E>, P, E>: CrdtBase<C, P, Set<E>>, MutableSet<E> {
}