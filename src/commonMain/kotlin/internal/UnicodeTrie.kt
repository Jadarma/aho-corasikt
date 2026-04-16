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

    // TODO: Handle match type and overlaps.
    override fun search(text: CharSequence, requireNoOverlaps: Boolean): Sequence<IntRange> = sequence {
        val source = text.iterator()
        val charMapper = caseSensitivity.asFunction()
        var current = nodes.first()
        var index = 0
        while (source.hasNext()) {
            val input = charMapper(source.next())
            current = current.next(input)
            var suffix = current.suffixNode
            while (suffix > 0) {
                val suffixNode = nodes[suffix]
                yield(index - suffixNode.depth + 1..index)
                suffix = suffixNode.suffixNode
            }
            if (current.isMatch) {
                yield(index - current.depth + 1..index)
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
