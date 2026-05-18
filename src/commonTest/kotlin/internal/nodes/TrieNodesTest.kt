package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.buildTrie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import io.github.jadarma.ahocorasikt.internal.trie.TrieImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests that the trie nodes created by the various optimizations perfectly match the theoretical model.
 * Example based on small trie featured on
 * [Wikipedia](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm#/media/File:A_diagram_of_the_Aho-Corasick_string_search_algorithm.svg).
 */
class TrieNodesTest : FunSpec({

    fun nodesByLevel(optimizationLevel: Int): TrieNodes {
        val trie = buildTrie {
            (this as TrieBuilderBase<*>).optimizationLevel = optimizationLevel
            addAll(listOf("a", "c", "ab", "bc", "bca", "caa"))
        }
        return (trie as TrieImpl).nodes
    }

    fun TrieNodes.shouldBeWiredCorrectly() {
        depth shouldBe 3
        size shouldBe 9

        // Node 0: Root
        isMatch(0).shouldBeFalse()
        depthOf(0) shouldBe 0
        suffixOf(0) shouldBe -1
        next(0, 'a'.code) shouldBe 1
        next(0, 'b'.code) shouldBe 2
        next(0, 'c'.code) shouldBe 3
        next(0, 'd'.code) shouldBe 0

        // Node1: A
        isMatch(1).shouldBeTrue()
        depthOf(1) shouldBe 1
        suffixOf(1) shouldBe -1
        next(1, 'a'.code) shouldBe 1
        next(1, 'b'.code) shouldBe 4
        next(1, 'c'.code) shouldBe 3
        next(1, 'd'.code) shouldBe 0

        // Node2: B
        isMatch(2).shouldBeFalse()
        depthOf(2) shouldBe 1
        suffixOf(2) shouldBe -1
        next(2, 'a'.code) shouldBe 1
        next(2, 'b'.code) shouldBe 2
        next(2, 'c'.code) shouldBe 5
        next(2, 'd'.code) shouldBe 0

        // Node3: C
        isMatch(3).shouldBeTrue()
        depthOf(3) shouldBe 1
        suffixOf(3) shouldBe -1
        next(3, 'a'.code) shouldBe 6
        next(3, 'b'.code) shouldBe 2
        next(3, 'c'.code) shouldBe 3
        next(3, 'd'.code) shouldBe 0

        // Node4: AB
        isMatch(4).shouldBeTrue()
        depthOf(4) shouldBe 2
        suffixOf(4) shouldBe -1
        next(4, 'a'.code) shouldBe 1
        next(4, 'b'.code) shouldBe 2
        next(4, 'c'.code) shouldBe 5
        next(4, 'd'.code) shouldBe 0

        // Node5: BC
        isMatch(5).shouldBeTrue()
        depthOf(5) shouldBe 2
        suffixOf(5) shouldBe 3
        next(5, 'a'.code) shouldBe 7
        next(5, 'b'.code) shouldBe 2
        next(5, 'c'.code) shouldBe 3
        next(5, 'd'.code) shouldBe 0

        // Node6: CA
        isMatch(6).shouldBeFalse()
        depthOf(6) shouldBe 2
        suffixOf(6) shouldBe 1
        next(6, 'a'.code) shouldBe 8
        next(6, 'b'.code) shouldBe 4
        next(6, 'c'.code) shouldBe 3
        next(6, 'd'.code) shouldBe 0

        // Node7: BCA
        isMatch(7).shouldBeTrue()
        depthOf(7) shouldBe 3
        suffixOf(7) shouldBe 1
        next(7, 'a'.code) shouldBe 8
        next(7, 'b'.code) shouldBe 4
        next(7, 'c'.code) shouldBe 3
        next(7, 'd'.code) shouldBe 0

        // Node8: CAA
        isMatch(8).shouldBeTrue()
        depthOf(8) shouldBe 3
        suffixOf(8) shouldBe 1
        next(8, 'a'.code) shouldBe 1
        next(8, 'b'.code) shouldBe 4
        next(8, 'c'.code) shouldBe 3
        next(8, 'd'.code) shouldBe 0
    }

    test("DefaultNodes") {
        nodesByLevel(0)
            .shouldBeInstanceOf<DefaultTrieNodes>()
            .shouldBeWiredCorrectly()
    }

    test("RemappedNodes") {
        nodesByLevel(1)
            .shouldBeInstanceOf<RemappedCompactTrieNodes>()
            .shouldBeWiredCorrectly()
    }

    test("CompactNodes") {
        nodesByLevel(2)
            .shouldBeInstanceOf<CompactTrieNodes>()
            .shouldBeWiredCorrectly()
    }
})
