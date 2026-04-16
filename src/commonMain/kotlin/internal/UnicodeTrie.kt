// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Trie

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
        require(size > 0) { "Trie must contain at least one node." }
    }

    // TODO: Handle overlaps.
    @Suppress("CognitiveComplexMethod")
    override fun search(text: CharSequence, requireNoOverlaps: Boolean): Sequence<IntRange> = sequence {
        val source = text.iterator()
        val charMapper = caseSensitivity.asFunction()
        val boundCheck = matchType.asFunction()

        var current = nodes.first()
        var index = 0

        fun testMatch(range: IntRange): Boolean {
            val before = text.getOrNull(range.first - 1)
            val after = text.getOrNull(range.last + 1)
            return (before == null || boundCheck(before)) && (after == null || boundCheck(after))
        }

        while (source.hasNext()) {
            val input = charMapper(source.next())
            current = current.next(input)
            var suffix = current.suffixNode
            while (suffix > 0) {
                val suffixNode = nodes[suffix]
                val range = index - suffixNode.depth + 1..index
                if(testMatch(range)) yield(range)
                suffix = suffixNode.suffixNode
            }
            if (current.isMatch) {
                val range = index - current.depth + 1..index
                if(testMatch(range)) yield(range)
            }
            index++
        }
    }

    private fun TrieNode.next(input: Char): TrieNode {
        var dest: TrieNode = this
        while (dest.index != 0 && !dest.children.containsKey(input)) {
            dest = nodes[dest.failNode]
        }
        return nodes[dest.children[input] ?: dest.failNode]
    }
}
