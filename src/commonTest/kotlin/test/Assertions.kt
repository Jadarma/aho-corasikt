package io.github.jadarma.ahocorasikt.test

import io.github.jadarma.ahocorasikt.Trie
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ranges.shouldBeWithin
import io.kotest.matchers.shouldBe

/** Assert that this trie can search the [text] for exactly the [expected] matches, in that order. */
fun Trie.shouldMatchExactly(
    text: CharSequence,
    expected: Iterable<Pair<IntRange, CharSequence>>,
    requireNoOverlaps: Boolean = false,
) {
    val expectedMatches = expected.map { (range, slice) ->
        withClue({ "The expected range $range is invalid or does not match text. Bad test declaration?" }) {
            (range as ClosedRange<Int>).shouldBeWithin(text.indices as ClosedRange<Int>)
            text.slice(range) shouldBe slice
        }
        range
    }

    val actualMatches = shouldNotThrowAny { search(text, requireNoOverlaps).toList() }

    withClue({"Some matches were out of bounds of text indices."}) {
        actualMatches.shouldForAll { (it as ClosedRange<Int>).shouldBeWithin(text.indices as ClosedRange<Int>) }
    }

    withClue({
        val actual = actualMatches.toSet()
        val expected = expectedMatches.toSet()
        val formatIndicesList = { ranges: Iterable<IntRange> ->
            ranges
                .joinToString(separator = "\n|") { " - $it => ${text.slice(it)}" }
                .ifEmpty { " - None, all good!" }
        }

        """
            |Expected but not found:
            |${formatIndicesList(expected - actual)}
            |Found but not expected:
            |${formatIndicesList(actual - expected)}
        """.trimMargin()
    }) {
        actualMatches shouldContainExactly expectedMatches
    }
}
