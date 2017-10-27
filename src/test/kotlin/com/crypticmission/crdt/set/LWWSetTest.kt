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
        val subject = LWWSet<String>(clock = clock)

        // when
        val value = subject.value()

        // then
        assertEquals(mutableSetOf<String>(), value)

    }

    @Test
    fun shouldBeContainElementsWhenTheyAreAdded() {
        // given
        val subject = LWWSet<String>(clock = clock)

        // when
        subject.add("a")

        subject.add("b")

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())
    }

    @Test
    fun shouldHonorElementRemovalsWhenElementsAreRemoved() {
        // given
        val subject = LWWSet<String>(clock = clock)

        // when
        subject.add("a")

        subject.add("b")

        assertEquals(mutableSetOf<String>("a", "b"), subject.value())

        subject.remove("a")

        // then
        assertEquals(mutableSetOf<String>("b"), subject.value())

    }


    @Test
    fun shouldContainAllElementsFromMergedSets() {
        // given
        val a = LWWSet<String>(clock = clock)
        val b = LWWSet<String>(clock = clock)

        // when
        a.add("a")

        b.add("b")

        val subject = a.merge(b)


        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())
    }

    @Test
    fun shouldHonorRemovalsFromAnother() {
        // given
        val a = LWWSet<String>(clock = clock)
        val b = LWWSet<String>(clock = clock)

        // when
        a.add("a")

        a.add("b")

        b.remove("b")

        val subject = a.merge(b)


        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())
    }

    @Test
    fun shouldHonorAddingBackFromAnotherSet() {
        // given
        val a = LWWSet<String>(clock = clock)
        val b = LWWSet<String>(clock = clock)

        // when
        a.add("a")

        a.add("b")

        b.remove("b")

        a.add("b")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>("a", "b"), subject.value())
    }

    @Test
    fun shouldHonorLastWriteWhenThereAreMultipleAdds() {
        // given
        val a = LWWSet<String>(clock = clock)
        val b = LWWSet<String>(clock = clock)

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
    }

    @Test
    fun shouldRemoveTiesWhenBiasedTowardsRemoving() {
        // given
        val a = LWWSet<String>(clock = clock, bias = Bias.TOWARDS_REMOVING)
        val b = LWWSet<String>(clock = clock, bias = Bias.TOWARDS_REMOVING)

        // when
        a.add("a")
        clock.autotick = false

        // these two events are simultaneous
        b.remove("a")
        b.add("a")

        val subject = a.merge(b)

        // then
        assertEquals(mutableSetOf<String>(), subject.value())
    }

    @Test
    fun shouldKeepTiesWhenBiasedTowardsAdding() {
        // given
        val a = LWWSet<String>(clock = clock, bias = Bias.TOWARDS_ADDING)
        val b = LWWSet<String>(clock = clock, bias = Bias.TOWARDS_ADDING)
        clock.autotick = false

        // when
        a.add("a")
        clock.tick()

        // these two events are simultaneous
        b.remove("a")
        b.add("a")

        println("a: ${a.payload}")
        println("b: ${b.payload}")

        val subject = a.merge(b)
        println("--------")
        println("a: ${a.payload}")
        println("b: ${b.payload}")
        println("subject: ${subject.payload}")

        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())
    }


    @Test
    fun shouldHonorBiasFromMergeReceiverKeepTiesWhenBiasesDiffer() {
        // given
        val a = LWWSet<String>(clock = clock, bias = Bias.TOWARDS_ADDING)
        val b = LWWSet<String>(clock = clock, bias = Bias.TOWARDS_REMOVING)
        clock.autotick = false

        // when
        a.add("a")
        clock.tick()

        // these two events are simultaneous
        b.remove("a")
        b.add("a")

        println("a: ${a.payload}")
        println("b: ${b.payload}")

        val subject = a.merge(b)
        println("--------")
        println("a: ${a.payload}")
        println("b: ${b.payload}")
        println("subject: ${subject.payload}")

        // then
        assertEquals(mutableSetOf<String>("a"), subject.value())
    }
}