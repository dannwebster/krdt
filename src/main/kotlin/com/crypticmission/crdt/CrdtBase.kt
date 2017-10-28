package com.crypticmission.crdt

import com.sun.org.apache.xml.internal.security.utils.Base64
import java.security.SecureRandom

/**
 */
interface CrdtBase<C : CrdtBase<C, P, V>, P, V> {
    abstract val clientId: String
    abstract val payload: P

    fun merge(other: C): C = merge(randomClientId(), other)
    abstract fun merge(clientId: String, other: C): C
    abstract fun value(): V
}

interface Counter {
    fun increment()
}

private val RANDOM = SecureRandom.getInstanceStrong()
private val MASK = 0x000000FF
private val SHIFTS = arrayOf<Int>(24, 16, 8, 0)

fun randomClientId()  = "krdt_" + Base64.encode(randomInt().toByteArray())

fun randomInt() : Int  = RANDOM.nextInt(0x40000000)

fun Int.toByteArray(): ByteArray = arrayOf<Byte>(
        this.getByte(0),
        this.getByte(1),
        this.getByte(2),
        this.getByte(3)
).toByteArray()

fun Int.getByte(i: Int) : Byte = ((this shr SHIFTS[i]) and MASK).toByte()

fun <K, V> Map<K, V>.merge(other: Map<K, V>, mergeFunction: (V, V?) -> V): MutableMap<K, V> {
    val new = mutableMapOf<K, V>()
    new.putAll(this)
    other.forEach { (otherK: K, otherV: V) ->
        new.compute(otherK) { _: K, thisV: V? -> mergeFunction(otherV, thisV) }
    }
    return new
}