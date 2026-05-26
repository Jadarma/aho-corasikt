package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.buildTrie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import io.github.jadarma.ahocorasikt.internal.trie.TrieImpl
import io.github.jadarma.ahocorasikt.internal.util.MapRemapper
import io.github.jadarma.ahocorasikt.internal.util.NoopRemapper
import io.github.jadarma.ahocorasikt.internal.util.Remapper
import io.github.jadarma.ahocorasikt.test.shouldMatchExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests that the trie nodes created by the various optimizations perfectly match the theoretical model.
 * Example based on small trie featured on [Wikipedia](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm).
 */
class TrieNodesTest : FunSpec({

    fun trieByLevel(optimizationLevel: Int): Trie = buildTrie {
        (this as TrieBuilderBase<*>).optimizationLevel = optimizationLevel
        addAll(listOf("a", "c", "ab", "bc", "bca", "caa"))
    }

    val text = "abccab"
    val expectedMatches = listOf(
        0..0 to "a",
        0..1 to "ab",
        1..2 to "bc",
        2..2 to "c",
        3..3 to "c",
        4..4 to "a",
        4..5 to "ab"
    )

    fun TrieNodes.shouldBeWiredCorrectly(remapper: Remapper) {
        depth shouldBe 3
        size shouldBe 9

        // Node 0: Root
        isMatch(0).shouldBeFalse()
        depthOf(0) shouldBe 0
        suffixOf(0) shouldBe -1
        next(0, remapper['a'.code]) shouldBe 1
        next(0, remapper['b'.code]) shouldBe 2
        next(0, remapper['c'.code]) shouldBe 3
        next(0, remapper['d'.code]) shouldBe 0

        // Node1: A
        isMatch(1).shouldBeTrue()
        depthOf(1) shouldBe 1
        suffixOf(1) shouldBe -1
        next(1, remapper['a'.code]) shouldBe 1
        next(1, remapper['b'.code]) shouldBe 4
        next(1, remapper['c'.code]) shouldBe 3
        next(1, remapper['d'.code]) shouldBe 0

        // Node2: B
        isMatch(2).shouldBeFalse()
        depthOf(2) shouldBe 1
        suffixOf(2) shouldBe -1
        next(2, remapper['a'.code]) shouldBe 1
        next(2, remapper['b'.code]) shouldBe 2
        next(2, remapper['c'.code]) shouldBe 5
        next(2, remapper['d'.code]) shouldBe 0

        // Node3: C
        isMatch(3).shouldBeTrue()
        depthOf(3) shouldBe 1
        suffixOf(3) shouldBe -1
        next(3, remapper['a'.code]) shouldBe 6
        next(3, remapper['b'.code]) shouldBe 2
        next(3, remapper['c'.code]) shouldBe 3
        next(3, remapper['d'.code]) shouldBe 0

        // Node4: AB
        isMatch(4).shouldBeTrue()
        depthOf(4) shouldBe 2
        suffixOf(4) shouldBe -1
        next(4, remapper['a'.code]) shouldBe 1
        next(4, remapper['b'.code]) shouldBe 2
        next(4, remapper['c'.code]) shouldBe 5
        next(4, remapper['d'.code]) shouldBe 0

        // Node5: BC
        isMatch(5).shouldBeTrue()
        depthOf(5) shouldBe 2
        suffixOf(5) shouldBe 3
        next(5, remapper['a'.code]) shouldBe 7
        next(5, remapper['b'.code]) shouldBe 2
        next(5, remapper['c'.code]) shouldBe 3
        next(5, remapper['d'.code]) shouldBe 0

        // Node6: CA
        isMatch(6).shouldBeFalse()
        depthOf(6) shouldBe 2
        suffixOf(6) shouldBe 1
        next(6, remapper['a'.code]) shouldBe 8
        next(6, remapper['b'.code]) shouldBe 4
        next(6, remapper['c'.code]) shouldBe 3
        next(6, remapper['d'.code]) shouldBe 0

        // Node7: BCA
        isMatch(7).shouldBeTrue()
        depthOf(7) shouldBe 3
        suffixOf(7) shouldBe 1
        next(7, remapper['a'.code]) shouldBe 8
        next(7, remapper['b'.code]) shouldBe 4
        next(7, remapper['c'.code]) shouldBe 3
        next(7, remapper['d'.code]) shouldBe 0

        // Node8: CAA
        isMatch(8).shouldBeTrue()
        depthOf(8) shouldBe 3
        suffixOf(8) shouldBe 1
        next(8, remapper['a'.code]) shouldBe 1
        next(8, remapper['b'.code]) shouldBe 4
        next(8, remapper['c'.code]) shouldBe 3
        next(8, remapper['d'.code]) shouldBe 0
    }

    test("DefaultNodes") {
        (trieByLevel(0) as TrieImpl).should {
            it.remapper.shouldBeInstanceOf<NoopRemapper>()
            it.nodes.shouldBeInstanceOf<DefaultTrieNodes>().shouldBeWiredCorrectly(it.remapper)
            it.shouldMatchExactly(text, expectedMatches)
        }
    }

    test("CompactRemappedNodes") {
        (trieByLevel(1) as TrieImpl).should {
            it.remapper.shouldBeInstanceOf<MapRemapper>()
            it.nodes.shouldBeInstanceOf<CompactTrieNodes>().shouldBeWiredCorrectly(it.remapper)
            it.shouldMatchExactly(text, expectedMatches)
        }
    }

    test("CompactNodes") {
        (trieByLevel(2) as TrieImpl).should {
            it.remapper.shouldBeInstanceOf<NoopRemapper>()
            it.nodes.shouldBeInstanceOf<CompactTrieNodes>().shouldBeWiredCorrectly(it.remapper)
            it.shouldMatchExactly(text, expectedMatches)
        }
    }

    test("OptimizedRemappedNodes") {
        (trieByLevel(3) as TrieImpl).should {
            it.remapper.shouldBeInstanceOf<MapRemapper>()
            it.nodes.shouldBeInstanceOf<OptimizedTrieNodes>().shouldBeWiredCorrectly(it.remapper)
            it.shouldMatchExactly(text, expectedMatches)
        }
    }

    test("OptimizedNodes") {
        (trieByLevel(4) as TrieImpl).should {
            it.remapper.shouldBeInstanceOf<NoopRemapper>()
            it.nodes.shouldBeInstanceOf<OptimizedTrieNodes>().shouldBeWiredCorrectly(it.remapper)
            it.shouldMatchExactly(text, expectedMatches)
        }
    }
})
