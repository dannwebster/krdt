package com.crypticmission.crdt.counter

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 */

class GCounterTest {

    @Test
    fun shouldProduceAZeroValueFromAnEmptyPayload() {
        // given
        val subject = GCounter("foo")

        // when
        val value = subject.value()

        // then
        assertEquals(0, value)
        assertEquals("foo", subject.clientId)
        assertEquals(mutableMapOf<String, Int>(), subject.payload)
    }

    @Test
    fun shouldProduceAValueMatchingIncrements() {
        // given
        val subject = GCounter("foo")

        // when
        subject.increment()

        // then
        assertEquals(1, subject.value())
        assertEquals(mutableMapOf<String, Int>("foo" to 1), subject.payload)

        // and when
        subject.increment()

        // and then
        assertEquals(2, subject.value())
        assertEquals(mutableMapOf<String, Int>("foo" to 2), subject.payload)
    }

    @Test
    fun shouldProduceCorrectValueWhenMultipleInstancesIncremented() {
        // given
        val a = GCounter("a")
        val b = GCounter("b")

        // when
        a.increment()
        a.increment()
        assertEquals(2, a.value())

        b.increment()
        b.increment()
        b.increment()
        assertEquals(3, b.value())

        val subject = a.merge(b)

        // then
        assertEquals(mutableMapOf("a" to 2, "b" to 3), subject.payload)
        assertEquals(5, subject.value())
    }

    @Test
    fun shouldProduceCorrectValueWhenMultipleInstancesInConflict() {
        // given
        val a = GCounter("a")
        val b = GCounter("b")

        // when
        a.increment()
        a.increment()
        assertEquals(2, a.value())

        b.increment()
        b.increment()
        b.increment()
        assertEquals(3, b.value())

        val merge1 = a.merge("c", b)

        merge1.increment()

        // then
        assertEquals(mutableMapOf("a" to 2, "b" to 3, "c" to 1), merge1.payload)
        assertEquals(6, merge1.value())

        // and when
        b.increment()
        b.increment()

        // and then
        val subject = merge1.merge(b)

        // then
        assertEquals(mutableMapOf("a" to 2, "b" to 5, "c" to 1), subject.payload)
        assertEquals(8, subject.value())
    }


    @Test
    fun shouldHandleAllDataClassMethods() {
        // given
        val subject = GCounter("a", mutableMapOf())
        val copy = subject.copy()

        val otherA = GCounter("a")
        otherA.increment()

        val otherB = GCounter("b")

        // when
        Assert.assertFalse(subject.equals(otherA))
        Assert.assertFalse(subject.equals(otherB))
        Assert.assertFalse(subject.equals(null))
        Assert.assertFalse(subject.equals("foo"))
        assertEquals(subject, copy)
        assertEquals(subject.hashCode(), copy.hashCode())
        assertEquals(subject.toString(), copy.toString())
        assertEquals(subject.component1(), copy.component1())
        assertEquals(subject.component2(), copy.component2())
    }
}