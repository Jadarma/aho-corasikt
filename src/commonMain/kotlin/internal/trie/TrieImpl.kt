// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.trie

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.asFunction
import io.github.jadarma.ahocorasikt.internal.nodes.TrieNodes
import io.github.jadarma.ahocorasikt.internal.overlap.Overlapper

internal open class TrieImpl(
    override val overlap: Overlap,
    override val matchType: MatchType,
    override val caseSensitivity: CaseSensitivity,
    internal val nodes: TrieNodes,
) : Trie {

    final override val size get() = nodes.size
    final override val depth get() = nodes.depth

    init {
        require(size > 1) { "Trie must contain at least one non-root node." }
        require(depth > 0) { "Trie depth must be positive." }
    }

    private val boundCheck = matchType.asFunction()
    private val charMapper = caseSensitivity.asFunction()

    private fun isRangeEligible(text: CharSequence, range: IntRange): Boolean {
        val before = text.getOrNull(range.first - 1)
        val after = text.getOrNull(range.last + 1)
        return (before == null || boundCheck(before)) && (after == null || boundCheck(after))
    }

    @Suppress("CognitiveComplexMethod") // Skill issue.
    protected inline fun <T : Any> search(
        text: CharSequence,
        requireNoOverlaps: Boolean,
        crossinline onOffer: (range: IntRange, node: Int) -> Unit,
        crossinline onConfirm: suspend SequenceScope<T>.(index: Int, ranges: List<IntRange>) -> Unit,
    ): Sequence<T> = sequence {
        val source = text.iterator()

        val overlapper = Overlapper.of(
            strategy = if (overlap == Overlap.Allowed && requireNoOverlaps) Overlap.Leftmost else overlap,
            trieDepth = depth,
        )

        var current = 0
        var index = 0

        while (source.hasNext()) {
            val input = charMapper(source.next()).code
            current = nodes.next(current, input)
            if (nodes.isMatch(current)) {
                val range = index - nodes.depthOf(current) + 1..index
                if (isRangeEligible(text, range)) {
                    overlapper.offer(range)
                    onOffer(range, current)
                }
            }
            var suffix = nodes.suffixOf(current)
            while (suffix > 0) {
                val range = index - nodes.depthOf(suffix) + 1..index
                if (isRangeEligible(text, range)) {
                    overlapper.offer(range)
                    onOffer(range, suffix)
                }
                suffix = nodes.suffixOf(suffix)
            }
            @Suppress("MagicNumber") // Tests that the index is a multiple of 256.
            if (index and 0xFF == 0) {
                onConfirm(index, overlapper.check(index))
            }
            index++
        }

        onConfirm(index, overlapper.finalCheck())
    }

    final override fun search(text: CharSequence, requireNoOverlaps: Boolean): Sequence<IntRange> =
        search(
            text = text,
            requireNoOverlaps = requireNoOverlaps,
            onOffer = { _, _ -> Unit },
            onConfirm = { _, ranges -> yieldAll(ranges) },
        )
}
