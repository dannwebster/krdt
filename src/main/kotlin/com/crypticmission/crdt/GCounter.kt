package com.crypticmission.crdt

/**
 */
data class GCounter(
        override val clientId: String = randomClientId(),
        override val payload: MutableMap<String, Int> = mutableMapOf()
) : CrdtBase<GCounter, Map<String, Int>, Int>, Counter {

    override fun value() : Int = payload.values.sum()

    override fun increment() : Unit { payload[clientId] = (payload[clientId] ?: 0) + 1 }

    override fun merge(clientId: String, other: GCounter): GCounter = GCounter(
            clientId = clientId,
            payload = this.payload.merge(other.payload) { i1: Int, i2: Int? -> maxOf(i1, (i2 ?: 0)) }
    )
}