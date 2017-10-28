package com.crypticmission.crdt.set

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 */
class ORSetTest {

    fun <E> ORSet<E>.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation() =
            (this as CachedCrdtBaseSet<*, *, *>).valueCalculationCount

    val versioner = DEFAULT_VERSIONER

    @Test
    fun shouldBeEmptyWhenCreated() {
        // given
        val subject = ORSet.new<String>(versioner = versioner)
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        val value = subject.value()

        // then
        assertEquals(mutableSetOf<String>(), value)
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

    }

    @Test
    fun shouldBeContainElementsWhenTheyAreAdded() {
        // given
        val subject = ORSet.new<String>(versioner = versioner)
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        subject.add("a")

        subject.add("b")

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldHonorElementRemovalsWhenElementsAreRemoved() {
        // given
        val subject = ORSet.new<String>(versioner = versioner)
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        subject.add("a")

        subject.add("b")

        assertEquals(mutableSetOf<String>("a", "b"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        subject.remove("a")

        // then
        assertEquals(mutableSetOf<String>("b"), subject.value())
        assertEquals(3, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

    }


    @Test
    fun shouldContainAllElementsFromMergedSets() {
        // given
        val a = ORSet.new<String>(versioner = versioner)
        val b = ORSet.new<String>(versioner = versioner)
        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        a.add("a")
        b.add("b")

        val subject = a.merge(b)
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())
        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldHonorRemovalsFromAnotherWhenMultipleSuccessiveRemoves() {

        // given
        val a = ORSet.new<String>(versioner = versioner)
        val b = ORSet.new<String>(versioner = versioner)

        // when
        a.add("a")

        // these two should cancel
        a.add("b")
        b.remove("b")
        b.remove("b")
        b.remove("b")

        a.add("b")
        val subject = a.merge(b)


        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldHonorAddingBackFromAnotherSet() {
        // given
        val a = ORSet.new<String>(versioner = versioner)
        val b = ORSet.new<String>(versioner = versioner)

        // when
        a.add("a")

        a.add("b")

        b.remove("b")

        a.add("b")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldContainOnlyItemsWithMoreAddsThanRemovesWhenUsingMultipleSets() {
        // given
        val a = ORSet.new<String>(versioner = versioner)
        val b = ORSet.new<String>(versioner = versioner)

        // when
        a.add("a")

        // cancelling
        a.add("b")
        b.remove("b")

        a.add("b")

        // cancelling
        b.add("b")
        a.remove("b")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldHaveOneElementWhenAddedAndRemovedAndAdded() {
        // given
        val a = ORSet.new<String>(versioner = versioner)
        val b = ORSet.new<String>(versioner = versioner)

        // when
        // these two cancel
        a.add("a")
        b.remove("a")

        b.add("a")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldKeepAddWhenAddAndRemoveAreTiedAndBiasedTowardsAdding() {
        // given
        val a = ORSet.new<String>(versioner = versioner)
        val b = ORSet.new<String>(versioner = versioner)

        // when
        a.add("a")

        // these two events are simultaneous
        b.remove("a")
        b.add("a")


        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }


    @Test
    fun shouldHonorBiasFromMergeReceiverAndNotMergeArgumentWhenBiasesDiffer() {
        // given
        val a = ORSet.new<String>(versioner = versioner)
        val b = ORSet.new<String>(versioner = versioner)

        // when
        a.add("a")

        // these two events are simultaneous
        b.remove("a")
        b.add("a")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }


    @Test
    fun shouldCalculateValueOnceMoreWhenAddingAll() {
        // given
        val subject = ORSet.new<String>()

        // when
        val allAdded1 = subject.addAll(setOf("a"))
        val allAdded2 = subject.addAll(setOf("a", "b", "c"))
        val allAdded3 = subject.addAll(setOf("a", "b", "c"))

        // then
        assertEquals(true, allAdded1)
        assertEquals(true, allAdded2)
        assertEquals(false, allAdded3)
        assertEquals(mutableSetOf<String>("a", "b", "c"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldCalculateValueOnceMoreWhenRemovingAll() {
        // given
        val subject = ORSet.fromSet(set = setOf("a", "b", "c"))

        // when
        val allRemoved1 = subject.removeAll(setOf("b"))
        val allRemoved2 = subject.removeAll(setOf("b", "c"))
        val allRemoved3 = subject.removeAll(setOf("b", "c"))

        // then
        assertEquals(true, allRemoved1)
        assertEquals(true, allRemoved2)
        assertEquals(false, allRemoved3)
        assertEquals(mutableSetOf<String>("a"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }


    @Test
    fun shouldCalculateValueOnceMoreRetainAll() {
        // given
        val subject = ORSet.fromSet(set = setOf("a", "b", "c"))
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        val allRetained1 = subject.retainAll(setOf("a", "b"))
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        val allRetained2 = subject.retainAll(setOf("a", "c"))
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // then
        assertEquals(true, allRetained1)
        assertEquals(true, allRetained2)
        assertEquals(mutableSetOf<String>("a"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldCorrectlyDetermineIfSomethingIsContainedWhenUsedInAnyCase() {
        // given
        val subject = ORSet.fromSet(set = setOf("a"))
        assertEquals(true, subject.contains("a"))

        // when
        subject.remove("a")
        assertEquals(false, subject.contains("a"))

        // then
        subject.add("a")
        assertEquals(true, subject.contains("a"))

    }

    @Test
    fun shouldBehaveLikeANormalSetWhenCleared() {
        // given
        val subject = ORSet.fromSet(clientId = "foo", set = setOf("a", "b", "c"))

        assertEquals("foo", subject.clientId)
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(true, subject.iterator().hasNext())
        assertEquals(true, subject.containsAll(setOf("a", "b", "c")))
        assertEquals(3, subject.size)
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        subject.clear()

        // then
        assertEquals(false, subject.containsAll(setOf("a", "b", "c")))
        assertEquals(true, subject.isEmpty())
        assertEquals(0, subject.size)
        assertEquals(false, subject.iterator().hasNext())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }


    @Test
    fun shouldHandleAllPayloadDataClassMethods() {
        // given
        val subject = ORPayload(
                toVersionedSet(mutableSetOf("a"), DEFAULT_VERSIONER),
                toVersionedSet(mutableSetOf("b"), DEFAULT_VERSIONER))

        val copy = subject.copy()

        val otherA = ORPayload(
                toVersionedSet(mutableSetOf("a"), DEFAULT_VERSIONER),
                toVersionedSet(mutableSetOf("a"), DEFAULT_VERSIONER))

        val otherB = ORPayload(
                toVersionedSet(mutableSetOf("a"), DEFAULT_VERSIONER),
                toVersionedSet(mutableSetOf("b"), DEFAULT_VERSIONER))

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