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

    fun requireFailure(function: () -> Unit) {
        try {
            function()
            fail("this should throw an exception")
        } catch (e: Exception ) {
            when (e) {
                is IllegalAccessException -> assertEquals("com.crypticmission.crdt.set.GSet is a grow-only set", e.message)
            }
        }
    }

    @Test
    fun shouldFailWhenTryingToRemove() {
        // given
        val subject = GSet("a", mutableSetOf("a"))

        // then
        requireFailure { -> subject.remove("a") }
        requireFailure { -> subject.removeAll(mutableSetOf("a")) }
        requireFailure { -> subject.removeIf { it == "a" } }

    }
}