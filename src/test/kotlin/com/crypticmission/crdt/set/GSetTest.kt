package com.crypticmission.crdt.set

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock

/**
 */
class GSetTest {
    @Test
    fun shouldBeEmptyWhenCreated() {
        // given
        val subject = GSet<String>("foo")

        // when
        val value = subject.payload

        // then
        assertEquals(setOf<String>(), value)
    }

    @Test
    fun shouldHaveBothDataPointsWhenMerged() {
        // given
        val a = GSet("a", mutableSetOf("a"))
        val b = GSet("b", mutableSetOf("b"))

        // when
        a.add("c")
        b.add("d")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf("a", "b", "c", "d"), subject.value())

    }

    @Test(expected = IllegalAccessException::class)
    fun shouldFailWhenTryingToRemove() {
        // given
        val subject = GSet("a", mutableSetOf("a"))

        // when
        subject.remove("a")
    }
    @Test(expected = IllegalAccessException::class)
    fun shouldFailWhenTryingToRemoveAll() {
        // given
        val subject = GSet("a", mutableSetOf("a"))

        // when
        subject.removeAll(mutableSetOf("a"))
    }

    @Test(expected = IllegalAccessException::class)
    fun shouldFailWhenTryingToRemoveAIf() {
        // given
        val subject = GSet("a", mutableSetOf("a"))
        subject.removeIf { it == "a" }
    }

    @Test(expected = IllegalAccessException::class)
    fun shouldFailWhenTryingToClear() {
        // given
        val subject = GSet("a", mutableSetOf("a"))
        subject.clear()
    }

    @Test
    fun shouldBehaveLikeANormalSet() {
        // given
        val subject = GSet(clientId = "foo", payload = mutableSetOf("a", "b", "c"))

        subject.addAll(setOf("d", "e"))

        assertEquals("foo", subject.clientId)
        assertEquals(true, subject.containsAll(setOf("a", "b", "c")))
        assertEquals(5, subject.toTypedArray().size)
        assertEquals(5, subject.size)
        assertEquals(true, subject.iterator().hasNext())

        assertEquals(false, subject.isEmpty())

    }

    @Test
    fun shouldHandleAllPayloadDataClassMethods() {
        // given
        val subject = GSet("foo", mutableSetOf("a"))

        val copy = subject.copy()

        val otherA = GSet("a", mutableSetOf("a"))
        val otherB = GSet("b", mutableSetOf("a"))

        // when
        assertFalse(subject.equals(otherA))
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