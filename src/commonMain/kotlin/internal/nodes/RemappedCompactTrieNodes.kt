// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderNode
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNode.Companion.MAX_COMPACT_SIZE

/**
 * Similar to [CompactTrieNodes], but uses a [remap] to reorder inputs.
 * This makes it possible to use it with any inputs, not just values under 256, provided the total trie alphabet is at
 * most 256 values.
 * Most single-language Unicode tries should be covered by this optimization.
 */
internal class RemappedCompactTrieNodes private constructor(
    private val nodes: CompactTrieNodes,
    private val remap: Map<Int, Int>,
) : TrieNodes by nodes {

    override fun next(index: Int, input: Int): Int {
        val actualInput = remap[input] ?: Int.MAX_VALUE
        return nodes.next(index, actualInput)
    }

    companion object {

        fun from(nodes: Array<TrieBuilderNode>, remap: Map<Int, Int>): RemappedCompactTrieNodes {
            require(remap.size <= MAX_COMPACT_SIZE) { "Invalid remap: too many inputs." }
            require(remap.values.all { it < MAX_COMPACT_SIZE }) { "Invalid remap: input values too large." }
            val compacted = Array(nodes.size) { CompactTrieNode.from(nodes[it], remap) }
            return RemappedCompactTrieNodes(CompactTrieNodes(compacted), remap)
        }
    }
}
