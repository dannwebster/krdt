package com.crypticmission.crdt.counter

import com.crypticmission.crdt.Counter
import com.crypticmission.crdt.CrdtBase
import com.crypticmission.crdt.randomClientId


data class PNPayload(val p: GCounter, val n: GCounter) {
    companion object {
        fun new() = PNPayload(GCounter(), GCounter())
    }
}
/**
 */
data class PNCounter(override val clientId: String = randomClientId(),
                     override val payload: PNPayload = PNPayload.new()) : CrdtBase<PNCounter, PNPayload, Int>, Counter {
    override fun value(): Int = payload.p.value() - payload.n.value()


    override fun increment() = payload.p.increment()
    fun decrement() = payload.n.increment()

    override fun merge(clientId: String, other: PNCounter): PNCounter = PNCounter(
            clientId = clientId,
            payload = PNPayload(
                    p = this.payload.p.merge(other.payload.p),
                    n = this.payload.n.merge(other.payload.n)
            )
    )
}