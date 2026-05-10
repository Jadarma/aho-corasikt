package io.github.jadarma.ahocorasikt.internal.overlap

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly

class IgnoredOverlapperTest : FunSpec({

    val overlapper = IgnoredOverlapper()
    val ranges = listOf(1..2,1..3, 2..3, 3..3, 6..9)

    test("Returns ranges in the order given.") {
        var yielded = mutableListOf<IntRange>()
        for(range in ranges) overlapper.offer(range)
        yielded += overlapper.check(10)
        yielded.shouldContainExactly(ranges)
        overlapper.check(100).shouldBeEmpty()
        overlapper.finalCheck().shouldBeEmpty()
    }
})
