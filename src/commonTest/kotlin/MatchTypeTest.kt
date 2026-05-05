package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.test.ofVariants
import io.github.jadarma.ahocorasikt.test.shouldMatchExactly
import io.kotest.core.spec.style.FunSpec

class MatchTypeTest : FunSpec({

    val text = "The quick-brown foxy jumps over the lazy!dog_@"
    val words = setOf("The", "the", "quick", "brown", "oxy", "jump", "over", "lazy", "dog", "@")

    context(MatchType.Unrestricted.name) {
        val ignored = Trie.ofVariants(words, matchType = MatchType.Unrestricted)
        val expected = listOf(
            0..2 to "The",
            4..8 to "quick",
            10..14 to "brown",
            17..19 to "oxy",
            21..24 to "jump",
            27..30 to "over",
            32..34 to "the",
            36..39 to "lazy",
            41..43 to "dog",
            45..45 to "@",
        )

        test("Emits all matches regardless of surroundings") {
            ignored.shouldMatchExactly(text, expected)
        }
    }

    context(MatchType.WhitespaceSeparated.name) {
        val ignored = Trie.ofVariants(words, matchType = MatchType.WhitespaceSeparated)
        val expected = listOf(
            0..2 to "The",
            27..30 to "over",
            32..34 to "the",
        )

        test("Emits only matches adjacent to whitespace") {
            ignored.shouldMatchExactly(text, expected)
        }
    }

    context(MatchType.WholeWords.name) {
        val ignored = Trie.ofVariants(words, matchType = MatchType.WholeWords)
        val expected = listOf(
            0..2 to "The",
            4..8 to "quick",
            10..14 to "brown",
            27..30 to "over",
            32..34 to "the",
            36..39 to "lazy",
            41..43 to "dog",
            45..45 to "@",
        )

        test("Emits only matches that are between non-letters.") {
            ignored.shouldMatchExactly(text, expected)
        }
    }
})
