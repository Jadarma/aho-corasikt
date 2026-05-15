package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.test.shouldMatchExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BuilderExtTest : FunSpec({

    val text = "Lorem ipsum dolor sit amet."
    val words = setOf("lorem", "dolor", "sit", "dolor sit", "et")
    val expectedSize = 20
    val expectedDepth = 9
    val expectedSensitivity = CaseSensitivity.AsciiInsensitive
    val expectedMatch = MatchType.WholeWords
    val expectedOverlap = Overlap.Longest

    fun Trie.shouldHaveExpectedPropertiesAndBehavior() {
        size shouldBe expectedSize
        depth shouldBe expectedDepth
        caseSensitivity shouldBe expectedSensitivity
        matchType shouldBe expectedMatch
        overlap shouldBe expectedOverlap
        shouldMatchExactly(text, listOf(
            0..4 to "Lorem",
            12..20 to "dolor sit",
        ))
    }

    fun PayloadTrie<Int>.shouldHaveExpectedPropertiesAndBehavior() {
        (this as Trie).shouldHaveExpectedPropertiesAndBehavior()
        searchWithPayload(text).toList() shouldBe listOf(
            0..4 to "Lorem".length,
            12..20 to "dolor sit".length,
        )
    }

    context("Trie builder extensions") {
        test("with builder action") {
            val trie = buildTrie(
                caseSensitivity = expectedSensitivity,
                matchType = expectedMatch,
                overlap = expectedOverlap,
            ) { addAll(words) }
            trie.shouldHaveExpectedPropertiesAndBehavior()
        }

        test("with existing collection") {
            val trie = trieOf(
                words = words,
                caseSensitivity = expectedSensitivity,
                matchType = expectedMatch,
                overlap = expectedOverlap,
            )
            trie.shouldHaveExpectedPropertiesAndBehavior()
        }

        test("with varargs") {
            val trie = trieOf(
                words = words.toTypedArray(),
                caseSensitivity = expectedSensitivity,
                matchType = expectedMatch,
                overlap = expectedOverlap,
            )
            trie.shouldHaveExpectedPropertiesAndBehavior()
        }
    }

    context("PayloadTrie builder extensions") {
        test("with builder action") {
            val trie = buildTrie<Int>(
                caseSensitivity = expectedSensitivity,
                matchType = expectedMatch,
                overlap = expectedOverlap,
            ) { for(word in words) add(word, word.length) }
            trie.shouldHaveExpectedPropertiesAndBehavior()
        }

        test("with existing collection") {
            val trie = trieOf(
                words = words.associateWith { it.length },
                caseSensitivity = expectedSensitivity,
                matchType = expectedMatch,
                overlap = expectedOverlap,
            )
            trie.shouldHaveExpectedPropertiesAndBehavior()
        }

        test("with varargs") {
            val trie = trieOf(
                words = words.map { it to it.length }.toTypedArray(),
                caseSensitivity = expectedSensitivity,
                matchType = expectedMatch,
                overlap = expectedOverlap,
            )
            trie.shouldHaveExpectedPropertiesAndBehavior()
        }
    }
})
