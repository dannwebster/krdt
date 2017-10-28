package com.crypticmission.crdt.set

import org.junit.Assert.*
import org.junit.Test

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
    fun shouldBehaveLikeANormalSetWhenCleared() {
        // given
        val subject = GSet(clientId = "foo", payload = mutableSetOf("a", "b", "c"))

        assertEquals("foo", subject.clientId)
        assertEquals(3, subject.size)
        assertEquals(true, subject.iterator().hasNext())
    }
}