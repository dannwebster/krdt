package com.crypticmission.crdt.counter

import com.crypticmission.crdt.set.DEFAULT_VERSIONER
import com.crypticmission.crdt.set.ORPayload
import com.crypticmission.crdt.set.toVersionedSet
import org.junit.Assert.*
import org.junit.Test

/**
 */
class PNCounterTest {
    @Test
    fun shouldHaveZeroValueWhenInitialized() {
        // given
        val subject = PNCounter()

        // when
        val value = subject.value()

        // then
        assertEquals(0, value)
        assertEquals(mutableMapOf<String, Int>(), subject.payload.p.payload)
        assertEquals(mutableMapOf<String, Int>(), subject.payload.n.payload)

    }

    @Test
    fun shouldProduceAValueMatchingIncrementAndDecrements() {
        // given
        val subject = PNCounter("foo")

        // when
        subject.increment()

        // then
        assertEquals(1, subject.value())

        // and when
        subject.decrement()

        // and then
        assertEquals(0, subject.value())
    }

    @Test
    fun shouldProduceCorrectValueWhenMultipleInstancesIncrementedAndDecremented() {
        // given
        val a = PNCounter("a")
        val b = PNCounter("b")

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
        assertEquals(5, subject.value())

        a.decrement()
        b.decrement()

        val subject2 = a.merge(b)
        assertEquals(3, subject2.value())
    }


    @Test
    fun shouldProduceCorrectValueWhenMultipleInstancesInConflict() {
        // given
        val a = PNCounter("a")
        val b = PNCounter("b")

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
        assertEquals(6, merge1.value())

        // and when
        b.decrement()
        b.decrement()

        // and then
        val subject = merge1.merge(b)

        // then
        assertEquals(4, subject.value())
    }

    @Test
    fun shouldHandleAllDataClassMethods() {
        // given
        val subject = PNCounter("a", PNPayload.new())
        val copy = subject.copy()

        val otherA1 = PNCounter("a")
        otherA1.increment()

        val otherA2 = PNCounter("a")
        otherA2.decrement()

        val otherB = PNCounter("b")

        // when
        assertFalse(subject.equals(otherA1))
        assertFalse(subject.equals(otherA2))
        assertFalse(subject.equals(otherB))
        assertFalse(subject.equals(null))
        assertFalse(subject.equals("foo"))
        assertEquals(subject, copy)
        assertEquals(subject.hashCode(), copy.hashCode())
        assertEquals(subject.toString(), copy.toString())
        assertEquals(subject.component1(), copy.component1())
        assertEquals(subject.component2(), copy.component2())
    }
}