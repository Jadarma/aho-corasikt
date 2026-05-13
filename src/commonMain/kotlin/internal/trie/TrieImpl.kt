// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.trie

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.asFunction
import io.github.jadarma.ahocorasikt.internal.nodes.TrieNodes
import io.github.jadarma.ahocorasikt.internal.overlap.Overlapper

internal class TrieImpl(
    override val overlap: Overlap,
    override val matchType: MatchType,
    override val caseSensitivity: CaseSensitivity,
    private val nodes: TrieNodes,
) : Trie, PayloadTrie<Nothing> {

    override val size get() = nodes.size
    override val depth get() = nodes.depth

    init {
        require(size > 1) { "Trie must contain at least one non-root node." }
        require(depth > 0) { "Trie depth must be positive." }
    }

    @Suppress("CognitiveComplexMethod") // Skill issue.
    override fun search(text: CharSequence, requireNoOverlaps: Boolean): Sequence<IntRange> = sequence {
        val source = text.iterator()
        val charMapper = caseSensitivity.asFunction()
        val boundCheck = matchType.asFunction()

        val overlapper = Overlapper.of(
            strategy = if(overlap == Overlap.Allowed && requireNoOverlaps) Overlap.Leftmost else overlap,
            trieDepth = depth,
        )

        var current = 0
        var index = 0

        fun testAndOffer(range: IntRange) {
            val before = text.getOrNull(range.first - 1)
            val after = text.getOrNull(range.last + 1)
            val isValidMatch = (before == null || boundCheck(before)) && (after == null || boundCheck(after))
            if(isValidMatch) overlapper.offer(range)
        }

        while (source.hasNext()) {
            val input = charMapper(source.next()).code
            current = nodes.next(current, input)
            if (nodes.isMatch(current)) {
                val range = index - nodes.depthOf(current) + 1..index
                testAndOffer(range)
            }
            var suffix = nodes.suffixOf(current)
            while (suffix > 0) {
                val range = index - nodes.depthOf(suffix) + 1..index
                testAndOffer(range)
                suffix = nodes.suffixOf(suffix)
            }
            if(index and 0xFF == 0) {
                yieldAll(overlapper.check(index))
            }
            index++
        }

        yieldAll(overlapper.finalCheck())
    }

    override fun contains(payload: Nothing) =
        throw UnsupportedOperationException("This trie was not built with payload supports.")

    override fun searchWithPayload(text: CharSequence, requireNoOverlaps: Boolean) =
        throw UnsupportedOperationException("This trie was not built with payload supports.")
}
