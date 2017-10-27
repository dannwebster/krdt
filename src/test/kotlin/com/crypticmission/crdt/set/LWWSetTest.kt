package com.crypticmission.crdt.set

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 */
class LWWSetTest {

    fun <E> LWWSet<E>.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation() =
            (this as CachedCrdtBaseSet<*, *, *>).valueCalculationCount

    data class SetableClock(var time : Instant = Instant.now(), var autotick : Boolean = true) : Clock() {
        override fun withZone(zone: ZoneId?): Clock = TODO("not implemented")
        override fun getZone(): ZoneId = TODO("not implemented")

        override fun instant(): Instant {
            val oldTime = time
            if (autotick) tick()
            return oldTime
        }

        fun tick() = inc(Duration.ofSeconds(1))

        fun inc(duration: Duration) {
            time = time.plus(duration)
        }
    }

    val clock = SetableClock(autotick = true);

    @Test
    fun shouldBeEmptyWhenCreated() {
        // given
        val subject = LWWSet.new<String>(clock = clock)
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
        val subject = LWWSet.new<String>(clock = clock)
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
        val subject = LWWSet.new<String>(clock = clock)
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
        val a = LWWSet.new<String>(clock = clock)
        val b = LWWSet.new<String>(clock = clock)
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
    fun shouldHonorRemovalsFromAnother() {
        // given
        val a = LWWSet.new<String>(clock = clock)
        val b = LWWSet.new<String>(clock = clock)

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
    fun shouldHonorAddingBackFromAnotherSet() {
        // given
        val a = LWWSet.new<String>(clock = clock)
        val b = LWWSet.new<String>(clock = clock)

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
    fun shouldHonorLastWriteWhenThereAreMultipleAdds() {
        // given
        val a = LWWSet.new<String>(clock = clock)
        val b = LWWSet.new<String>(clock = clock)

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
    fun shouldKeepRemoveWhenAddAndRemoveAreTiedAndBiasedTowardsRemoving() {
        // given
        val a = LWWSet.new<String>(clock = clock, bias = Bias.TOWARDS_REMOVING)
        val b = LWWSet.new<String>(clock = clock, bias = Bias.TOWARDS_REMOVING)

        // when
        a.add("a")
        clock.autotick = false

        // these two events are simultaneous
        b.remove("a")
        b.add("a")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>(), subject.value())

        assertEquals(1, a.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, b.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
        assertEquals(1, subject.valueCalculatedOnceAtCreationAndOnceWhenGettingAfterMutation())
    }

    @Test
    fun shouldKeepAddWhenAddAndRemoveAreTiedAndBiasedTowardsAdding() {
        // given
        val a = LWWSet.new<String>(clock = clock, bias = Bias.TOWARDS_ADDING)
        val b = LWWSet.new<String>(clock = clock, bias = Bias.TOWARDS_ADDING)
        clock.autotick = false

        // when
        a.add("a")
        clock.tick()

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
        val a = LWWSet.new<String>(clock = clock, bias = Bias.TOWARDS_ADDING)
        val b = LWWSet.new<String>(clock = clock, bias = Bias.TOWARDS_REMOVING)
        clock.autotick = false

        // when
        a.add("a")
        clock.tick()

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
        val subject = LWWSet.new<String>()

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
        val subject = LWWSet.fromSet(set = setOf("a", "b", "c"))

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
        val subject = LWWSet.fromSet(set = setOf("a", "b", "c"))
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
}