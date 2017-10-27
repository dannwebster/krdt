package com.crypticmission.crdt

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 */
class CrdtBaseTest {
    @Test
    fun shouldSeparateIntoBytesWhenCallingToByteArray() {
        // given
        val subject: Int = 0x44556677

        // when
        val value: ByteArray = subject.toByteArray()

        // then
        assertEquals(0x44.toByte(), value[0])
        assertEquals(0x55.toByte(), value[1])
        assertEquals(0x66.toByte(), value[2])
        assertEquals(0x77.toByte(), value[3])

    }

    @Test
    fun shouldMergeMapsWhenMergeMapCalled() {
        // given
        val a = mapOf("a" to 1, "b" to 3)
        val b = mapOf("a" to 2, "b" to 4)

        // when
        val subject = a.merge(b) { i1: Int, i2: Int? -> i1 + (i2 ?: 0)}

        // then
        assertEquals(mutableMapOf("a" to 3, "b" to 7), subject)

    }
}