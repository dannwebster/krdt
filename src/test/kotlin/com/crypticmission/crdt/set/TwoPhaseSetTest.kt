package com.crypticmission.crdt.set

import org.junit.Assert.*
import org.junit.Test

/**
 */
class TwoPhaseSetTest {

    fun <E> TwoPhaseSet<E>.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation() =
            (this as CachedCrdtBaseSet<*, *, *>).valueCalculationCount

    @Test
    fun shouldBeEmptyWhenCreated() {
        // given
        val subject = TwoPhaseSet.new<String>()

        // when
        val value = subject.value()

        // then
        assertEquals(mutableSetOf<String>(), value)

        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldContainElementsWhenTheyAreAdded() {
        // given
        val subject = TwoPhaseSet.new<String>()
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        subject.add("a")

        subject.add("b")
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())

        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldHonorElementRemovalsWhenElementsAreRemoved() {
        // given
        val subject = TwoPhaseSet.new<String>()

        // when
        subject.add("a")

        subject.add("b")

        assertEquals(mutableSetOf<String>("a", "b"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        subject.remove("a")
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // then
        assertEquals(mutableSetOf<String>("b"), subject.value())

        assertEquals(3, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }


    @Test
    fun shouldContainAllElementsFromMergedSets() {
        // given
        val a = TwoPhaseSet.new<String>()
        val b = TwoPhaseSet.new<String>()

        // when
        a.add("a")

        b.add("b")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldHonorRemovalsFromAnother() {
        // given
        val a = TwoPhaseSet.new<String>()
        val b = TwoPhaseSet.new<String>()

        // when
        a.add("a")

        a.add("b")

        b.remove("b")

        val subject = a.merge(b)


        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldHonorLastWriteWhenThereAreMultipleAdds() {
        // given
        val a = TwoPhaseSet.new<String>()
        val b = TwoPhaseSet.new<String>()

        // when
        a.add("a")

        a.add("b")

        b.remove("b")

        a.add("b")
        b.add("b")

        a.remove("b")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldNotReAddItemWhenItemHasBeenRemoved() {
        // given
        val a = TwoPhaseSet.new<String>()

        // when
        a.add("a")

        // these two events are simultaneous
        a.remove("a")
        a.add("a")

        // then
        assertEquals(mutableSetOf<String>(), a.value())

        assertEquals(2, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldNotReAddItemWhenItemHasBeenRemovedInOtherCollection() {
        // given
        val a = TwoPhaseSet.new<String>()
        val b = TwoPhaseSet.new<String>()

        // when
        val added1 = a.add("a")

        b.remove("a")
        val added2 = a.add("a")


        val subject = a.merge(b)

        val added3 = subject.add("a")

        // then
        assertEquals(true, added1)
        assertEquals(false, added2)
        assertEquals(false, added3)
        assertEquals(mutableSetOf<String>(), subject.value())
        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldCalculateValueOnceMoreWhenAddingAll() {
        // given
        val subject = TwoPhaseSet.new<String>()

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
        val subject = TwoPhaseSet.fromSet(set = setOf("a", "b", "c"))

        // when
        val allRemoved1 = subject.removeAll(setOf("b"))
        val allRemoved2 = subject.removeAll(setOf("b", "c"))

        // then
        assertEquals(true, allRemoved1)
        assertEquals(true, allRemoved2)
        assertEquals(mutableSetOf<String>("a"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }


    @Test
    fun shouldCalculateValueOnceMoreRetainAll() {
        // given
        val subject = TwoPhaseSet.fromSet(set = setOf("a", "b", "c"))
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())

        // when
        val allRetained1 = subject.retainAll(setOf("a", "b"))
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        val allRetained2 = subject.retainAll(setOf("a", "c"))
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        val allRetained3 = subject.retainAll(setOf("a", "b"))

        // then
        assertEquals(true, allRetained1)
        assertEquals(true, allRetained2)
        assertEquals(false, allRetained3)
        assertEquals(mutableSetOf<String>("a"), subject.value())
        assertEquals(2, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldBehaveLikeANormalSetWhenCleared() {
        // given
        val subject = TwoPhaseSet.fromSet(clientId = "foo", set = setOf("a", "b", "c"))

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
        val subject = TwoPhasePayload(
                GSet(payload = mutableSetOf("a")),
                GSet(payload = mutableSetOf("b")))

        val copy = subject.copy()

        val otherA = TwoPhasePayload(
                GSet(payload = mutableSetOf("a")),
                GSet(payload = mutableSetOf("a")))

        val otherB = TwoPhasePayload(
                GSet(payload = mutableSetOf("b")),
                GSet(payload = mutableSetOf("b")))

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
