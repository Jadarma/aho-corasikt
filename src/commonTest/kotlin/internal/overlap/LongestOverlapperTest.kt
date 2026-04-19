package io.github.jadarma.ahocorasikt.internal.overlap

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class LongestOverlapperTest : FunSpec({

    test("Ignores no overlaps") {
        with(LongestOverlapper(buffer = 4)) {
            offer(1..1)
            offer(2..3)
            offer(4..5)
            offer(6..8)
            finalCheck() shouldContainExactly listOf(1..1, 2..3, 4..5, 6..8)
        }
    }

    test("Can find subproblems when checking") {
        with(LongestOverlapper(buffer = 4)) {
            offer(1..1)
            offer(2..3)
            offer(1..4)
            check(6).shouldBeEmpty()
            offer(6..7)
            offer(7..7)
            check(9).shouldBeSingleton { it shouldBe 1..4 }
            finalCheck().shouldBeSingleton { it shouldBe 6..7 }
        }
    }

    test("A longer overlap might be overruled by an even bigger one") {
        with(LongestOverlapper(buffer = 4)) {
            offer(1..3)
            offer(2..4)
            offer(4..5)
            check(5).shouldBeEmpty()
            check(10).shouldContainExactly(1..3, 4..5)
            finalCheck().shouldBeEmpty()
        }
    }

    test("An long chain of overlaps can be resolved incrementally.") {
        with(LongestOverlapper(buffer = 5)) {
            offer(1..2)
            offer(2..4)
            offer(4..6)
            offer(6..8)
            check(8).shouldBeEmpty()
            offer(8..10)
            offer(10..12)
            offer(12..15)
            check(16).shouldContainExactly(2..4, 6..8)
            finalCheck().shouldBeSingleton { it shouldBe 12..15 }
        }
    }
})
