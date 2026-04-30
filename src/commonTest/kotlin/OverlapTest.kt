package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.test.shouldMatchExactly
import io.github.jadarma.ahocorasikt.test.shouldMatchExactlyIgnoringOverlaps
import io.kotest.core.spec.style.FunSpec

class OverlapTest : FunSpec({

    val text = "The bobcat is a cute cat, and also industrial machinery!!"
    val words = setOf("bob", "cat", "bobcat", "cut", "cute", "in", "dust", "trial", "mac", "chin", "hinery")

    val withOverlaps = listOf(
        4..6 to "bob",
        4..9 to "bobcat",
        7..9 to "cat",
        16..18 to "cut",
        16..19 to "cute",
        21..23 to "cat",
        35..36 to "in",
        37..40 to "dust",
        40..44 to "trial",
        46..48 to "mac",
        48..51 to "chin",
        50..51 to "in",
        49..54 to "hinery",
    )

    val withLeftmost = listOf(
        4..9 to "bobcat", // Takes over `cat` because it starts before, and over `bob` because it is longer.
        16..19 to "cute", // Takes over `cut` because they have the same start but this is longer.
        21..23 to "cat",
        35..36 to "in",
        37..40 to "dust", // Takes over `trial` because even though it is longer, this match starts first.
        46..48 to "mac", // Takes over `chin` because even though it is longer, this match starts first.
        49..54 to "hinery", // Takes over `in` because even though `chin` would've won, it was eliminated by `mac`.
    )

    val withLongest = listOf(
        4..9 to "bobcat", // Takes over `bob` and `cat` because its longer.
        16..19 to "cute", // Takes over `cut` because it is longer.
        21..23 to "cat",
        35..36 to "in",
        40..44 to "trial", // Takes over `dust` because it is longer.
        46..48 to "mac", // Would've lost to `chin`, but `chin` later loses to `hinery`, so it is "revived".
        49..54 to "hinery", // Takes over `chin` because it is longer.
    )

    context(Overlap.Allowed.name) {
        val allowed = Trie.of(words, overlap = Overlap.Allowed)
        test("Emits all matches ignoring overlaps") {
            allowed.shouldMatchExactly(text, withOverlaps)
        }
        test("Falls back to leftmost when required no overlaps.") {
            allowed.shouldMatchExactlyIgnoringOverlaps(text, withLeftmost)
        }
    }

    context(Overlap.Leftmost.name) {
        val ignored = Trie.of(words, overlap = Overlap.Leftmost)
        test("Prefers earlier matches, then longer matches.") {
            ignored.shouldMatchExactly(text, withLeftmost)
        }
        test("Is unaffected by no overlaps request, it is the same strategy") {
            ignored.shouldMatchExactlyIgnoringOverlaps(text, withLeftmost)
        }
    }

    context(Overlap.Longest.name) {
        val ignored = Trie.of(words, overlap = Overlap.Longest)
        test("Prefers longer matches.") {
            ignored.shouldMatchExactly(text, withLongest)
        }
        test("Is unaffected by no overlaps request, it already avoids overlaps") {
            ignored.shouldMatchExactlyIgnoringOverlaps(text, withLongest)
        }
    }
})
