package io.github.jadarma.ahocorasikt.internal.overlap

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class LeftmostOverlapperTest : FunSpec({

    test("Buffers candidates") {
        with(LeftmostOverlapper(buffer = 4)) {
            offer(1..1)
            offer(1..2)
            check(2).shouldBeEmpty()
            offer(2..3)
            check(3).shouldBeEmpty()
            offer(2..4)
            check(8).shouldBeSingleton { it shouldBe 1..2 }
            offer(6..8)
            offer(7..9)
            finalCheck().shouldBeSingleton { it shouldBe 6..8 }
        }
    }

    test("Can find subproblems when checking") {
        with(LongestOverlapper(buffer = 4)) {
            offer(1..1)
            offer(2..3)
            offer(3..4)
            check(6).shouldBeSingleton { it shouldBe 1..1 }
            check(10).shouldBeSingleton { it shouldBe 2..3 }
            finalCheck().shouldBeEmpty()
        }
    }

    test("Ignores no overlaps") {
        with(LeftmostOverlapper(buffer = 4)) {
            offer(1..1)
            offer(2..3)
            offer(4..5)
            offer(6..8)
            finalCheck() shouldContainExactly listOf(1..1, 2..3, 4..5, 6..8)
        }
    }

    test("Deals with subword edge-case") {
        with(LeftmostOverlapper(buffer = 6)) {
            offer(0..0)
            offer(1..3)
            offer(4..5)
            offer(1..6)
            check(6).shouldBeSingleton { it shouldBe 0..0 }
            finalCheck().shouldBeSingleton { it shouldBe 1..6 }
        }
    }
})
