// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.overlap.Overlapper

/**
 * A trie that can handle the full Unicode range.
 * It is stored as a flat array of nodes, with their child connections encoded as simple hashmaps.
 */
internal class UnicodeTrie(
    override val overlap: Overlap,
    override val matchType: MatchType,
    override val caseSensitivity: CaseSensitivity,
    override val depth: Int,
    private val nodes: Array<TrieNode>,
) : Trie {

    override val size get() = nodes.size

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

        var current = nodes.first()
        var index = 0
        val yieldEvery = 256

        fun testAndOffer(range: IntRange) {
            val before = text.getOrNull(range.first - 1)
            val after = text.getOrNull(range.last + 1)
            val isValidMatch = (before == null || boundCheck(before)) && (after == null || boundCheck(after))
            if(isValidMatch) overlapper.offer(range)
        }

        while (source.hasNext()) {
            val input = charMapper(source.next())
            current = current.next(input)
            var suffix = current.suffixNode
            if (current.isMatch) {
                val range = index - current.depth + 1..index
                testAndOffer(range)
            }
            while (suffix > 0) {
                val suffixNode = nodes[suffix]
                val range = index - suffixNode.depth + 1..index
                testAndOffer(range)
                suffix = suffixNode.suffixNode
            }
            if(index % yieldEvery == 0) {
                yieldAll(overlapper.check(index))
            }
            index++
        }

        yieldAll(overlapper.finalCheck())
    }

    private fun TrieNode.next(input: Char): TrieNode {
        var dest: TrieNode = this
        while (dest.index != 0 && !dest.children.containsKey(input)) {
            dest = nodes[dest.failNode]
        }
        return nodes[dest.children[input] ?: dest.failNode]
    }
}
