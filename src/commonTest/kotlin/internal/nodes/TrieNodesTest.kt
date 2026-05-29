package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.buildTrie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase.TrieVariant
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

    fun trieByLevel(variant: TrieVariant, words: Collection<CharSequence>): TrieImpl = buildTrie {
        (this as TrieBuilderBase<*>).forceVariant = variant
        addAll(words)
    } as TrieImpl

    val normalText = "abccab"
    val normalInputs = "abcd"
    val normalWords = listOf("a", "c", "ab", "bc", "bca", "caa")
    val expectedNormalMatches = listOf(
        0..0 to "a",
        0..1 to "ab",
        1..2 to "bc",
        2..2 to "c",
        3..3 to "c",
        4..4 to "a",
        4..5 to "ab"
    )

    // Same as above, but using greek letters, which have higher code values and force the builder to use remappers.
    val remappedText = "αβγγαβ"
    val remappedInputs = "αβγδ"
    val remappedWords = listOf("α", "γ", "αβ", "βγ", "βγα", "γαα")
    val expectedRemappedMatches = listOf(
        0..0 to "α",
        0..1 to "αβ",
        1..2 to "βγ",
        2..2 to "γ",
        3..3 to "γ",
        4..4 to "α",
        4..5 to "αβ"
    )

    fun TrieNodes.shouldBeWiredCorrectly(remapper: Remapper, inputs: CharSequence) {
        depth shouldBe 3
        size shouldBe 9

        // Node 0: Root
        isMatch(0).shouldBeFalse()
        depthOf(0) shouldBe 0
        suffixOf(0) shouldBe -1
        next(0, remapper[inputs[0].code]) shouldBe 1
        next(0, remapper[inputs[1].code]) shouldBe 2
        next(0, remapper[inputs[2].code]) shouldBe 3
        next(0, remapper[inputs[3].code]) shouldBe 0

        // Node1: A
        isMatch(1).shouldBeTrue()
        depthOf(1) shouldBe 1
        suffixOf(1) shouldBe -1
        next(1, remapper[inputs[0].code]) shouldBe 1
        next(1, remapper[inputs[1].code]) shouldBe 4
        next(1, remapper[inputs[2].code]) shouldBe 3
        next(1, remapper[inputs[3].code]) shouldBe 0

        // Node2: B
        isMatch(2).shouldBeFalse()
        depthOf(2) shouldBe 1
        suffixOf(2) shouldBe -1
        next(2, remapper[inputs[0].code]) shouldBe 1
        next(2, remapper[inputs[1].code]) shouldBe 2
        next(2, remapper[inputs[2].code]) shouldBe 5
        next(2, remapper[inputs[3].code]) shouldBe 0

        // Node3: C
        isMatch(3).shouldBeTrue()
        depthOf(3) shouldBe 1
        suffixOf(3) shouldBe -1
        next(3, remapper[inputs[0].code]) shouldBe 6
        next(3, remapper[inputs[1].code]) shouldBe 2
        next(3, remapper[inputs[2].code]) shouldBe 3
        next(3, remapper[inputs[3].code]) shouldBe 0

        // Node4: AB
        isMatch(4).shouldBeTrue()
        depthOf(4) shouldBe 2
        suffixOf(4) shouldBe -1
        next(4, remapper[inputs[0].code]) shouldBe 1
        next(4, remapper[inputs[1].code]) shouldBe 2
        next(4, remapper[inputs[2].code]) shouldBe 5
        next(4, remapper[inputs[3].code]) shouldBe 0

        // Node5: BC
        isMatch(5).shouldBeTrue()
        depthOf(5) shouldBe 2
        suffixOf(5) shouldBe 3
        next(5, remapper[inputs[0].code]) shouldBe 7
        next(5, remapper[inputs[1].code]) shouldBe 2
        next(5, remapper[inputs[2].code]) shouldBe 3
        next(5, remapper[inputs[3].code]) shouldBe 0

        // Node6: CA
        isMatch(6).shouldBeFalse()
        depthOf(6) shouldBe 2
        suffixOf(6) shouldBe 1
        next(6, remapper[inputs[0].code]) shouldBe 8
        next(6, remapper[inputs[1].code]) shouldBe 4
        next(6, remapper[inputs[2].code]) shouldBe 3
        next(6, remapper[inputs[3].code]) shouldBe 0

        // Node7: BCA
        isMatch(7).shouldBeTrue()
        depthOf(7) shouldBe 3
        suffixOf(7) shouldBe 1
        next(7, remapper[inputs[0].code]) shouldBe 8
        next(7, remapper[inputs[1].code]) shouldBe 4
        next(7, remapper[inputs[2].code]) shouldBe 3
        next(7, remapper[inputs[3].code]) shouldBe 0

        // Node8: CAA
        isMatch(8).shouldBeTrue()
        depthOf(8) shouldBe 3
        suffixOf(8) shouldBe 1
        next(8, remapper[inputs[0].code]) shouldBe 1
        next(8, remapper[inputs[1].code]) shouldBe 4
        next(8, remapper[inputs[2].code]) shouldBe 3
        next(8, remapper[inputs[3].code]) shouldBe 0
    }

    test("DefaultNodes") {
        trieByLevel(TrieVariant.Unoptimized, normalWords).should {
            it.remapper.shouldBeInstanceOf<NoopRemapper>()
            it.nodes.shouldBeInstanceOf<DefaultTrieNodes>().shouldBeWiredCorrectly(it.remapper, normalInputs)
            it.shouldMatchExactly(normalText, expectedNormalMatches)
        }
        trieByLevel(TrieVariant.Unoptimized, remappedWords).should {
            it.remapper.shouldBeInstanceOf<NoopRemapper>() // Default nodes don't need remapping.
            it.nodes.shouldBeInstanceOf<DefaultTrieNodes>().shouldBeWiredCorrectly(it.remapper, remappedInputs)
            it.shouldMatchExactly(remappedText, expectedRemappedMatches)
        }
    }

    test("CompactNodes") {
        trieByLevel(TrieVariant.Compact, normalWords).should {
            it.remapper.shouldBeInstanceOf<NoopRemapper>()
            it.nodes.shouldBeInstanceOf<CompactTrieNodes>().shouldBeWiredCorrectly(it.remapper, normalInputs)
            it.shouldMatchExactly(normalText, expectedNormalMatches)
        }
        trieByLevel(TrieVariant.Compact, remappedWords).should {
            it.remapper.shouldBeInstanceOf<MapRemapper>()
            it.nodes.shouldBeInstanceOf<CompactTrieNodes>().shouldBeWiredCorrectly(it.remapper, remappedInputs)
            it.shouldMatchExactly(remappedText, expectedRemappedMatches)
        }
    }

    test("OptimizedNodes") {
        trieByLevel(TrieVariant.Optimized, normalWords).should {
            it.remapper.shouldBeInstanceOf<NoopRemapper>()
            it.nodes.shouldBeInstanceOf<OptimizedTrieNodes>().shouldBeWiredCorrectly(it.remapper, normalInputs)
            it.shouldMatchExactly(normalText, expectedNormalMatches)
        }
        trieByLevel(TrieVariant.Optimized, remappedWords).should {
            it.remapper.shouldBeInstanceOf<MapRemapper>()
            it.nodes.shouldBeInstanceOf<OptimizedTrieNodes>().shouldBeWiredCorrectly(it.remapper, remappedInputs)
            it.shouldMatchExactly(remappedText, expectedRemappedMatches)
        }
    }
})
