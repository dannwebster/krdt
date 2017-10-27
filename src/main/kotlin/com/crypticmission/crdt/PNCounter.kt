package com.crypticmission.crdt


data class PN(val p: GCounter, val n: GCounter) {
    companion object {
        fun new() = PN(GCounter(), GCounter())
    }
}
/**
 */
data class PNCounter(override val clientId: String = randomClientId(),
                     override val payload: PN = PN.new()) : CrdtBase<PNCounter, PN, Int>, Counter {
    override fun value(): Int = payload.p.value() - payload.n.value()


    override fun increment() = payload.p.increment()
    fun decrement() = payload.n.increment()

    override fun merge(clientId: String, other: PNCounter): PNCounter = PNCounter(
            clientId = clientId,
            payload = PN(
                    p = this.payload.p.merge(other.payload.p),
                    n = this.payload.n.merge(other.payload.n)
            )
    )
}