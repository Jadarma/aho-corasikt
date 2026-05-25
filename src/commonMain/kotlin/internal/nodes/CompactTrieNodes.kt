// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderNode
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNode.Companion.MAX_COMPACT_SIZE
import io.github.jadarma.ahocorasikt.internal.util.Remapper
import kotlin.math.absoluteValue

/**
 * Stores nodes in a breadth-first-ordered array.
 * The children of nodes are not stored directly, rather each node remembers the index of the first child node, and
 * has a bitset, where the _n_-th bit is set if a connection exists to a child node via _n_.
 * The actual index of the child is calculated by adding the pop-count of bits left of n, and adding that to the index
 * of the first child.
 *
 * This optimization improves performance as there are no hashmap calculations to maintain, and the node itself is
 * contiguous in memory, reducing memory lookup.
 * However, it may only be used when all inputs are in `0..255` range.
 */
@JvmInline
internal value class CompactTrieNodes(val nodes: Array<CompactTrieNode>) : TrieNodes {

    override val size: Int get() = nodes.size

    override val depth: Int get() = nodes.last().depth

    override fun isMatch(index: Int): Boolean = nodes[index].isMatch

    override fun depthOf(index: Int): Int = nodes[index].depth

    override fun suffixOf(index: Int): Int = nodes[index].suffixNode

    override fun next(index: Int, input: Int): Int {
        if (input >= MAX_COMPACT_SIZE) return 0
        var node = index
        while (true) {
            val next = nodes[node].next(input)
            if (next >= node) return next
            node = next
        }
    }

    companion object {

        fun from(nodes: Array<TrieBuilderNode>, remapper: Remapper): CompactTrieNodes {
            val compacted = Array(nodes.size) { CompactTrieNode.from(nodes[it], remapper) }
            return CompactTrieNodes(compacted)
        }
    }
}

@Suppress("MagicNumber")
internal class CompactTrieNode(
    val depthAndMatch: Int,
    val suffixNode: Int,
    val failNode: Int,
    val childOffset: Int,
    val children0: Long,
    val children1: Long,
    val children2: Long,
    val children3: Long,
) {

    val depth: Int get() = depthAndMatch.absoluteValue
    val isMatch: Boolean get() = depthAndMatch > 0

    fun next(input: Int): Int {
        val bucketIndex = input.ushr(6)

        var offset = childOffset
        var bucket = children0

        if (bucketIndex > 0) { offset += bucket.countOneBits(); bucket = children1 }
        if (bucketIndex > 1) { offset += bucket.countOneBits(); bucket = children2 }
        if (bucketIndex > 2) { offset += bucket.countOneBits(); bucket = children3 }

        val bucketOffset = 63 - input.rem(64)
        if (!bucket.bitAt(bucketOffset)) return failNode
        if (bucketOffset < 63) offset += (bucket ushr bucketOffset.inc()).countOneBits()
        return offset
    }

    companion object {

        /**
         * The maximum number of children a [CompactTrieNode] can have.
         * Values of all child connections (input value transitions) must also be lower than this value.
         */
        const val MAX_COMPACT_SIZE = 256

        fun from(node: TrieBuilderNode, remap: Remapper): CompactTrieNode {
            require(node.children.size <= MAX_COMPACT_SIZE) { "Cannot compact node. It has too many children." }

            val children = LongArray(4).apply {
                for (via in node.children.keys) {
                    val input = remap.getValue(via)
                    val bucket = input.ushr(6)
                    val bucketOffset = 63 - input.rem(64)
                    this[bucket] = this[bucket].setBit(bucketOffset)
                }
            }

            return CompactTrieNode(
                depthAndMatch = node.depth * if (node.isMatch) 1 else -1,
                suffixNode = node.suffixNode?.index ?: -1,
                failNode = node.failNode.index,
                childOffset = node.children.minByOrNull { it.key }?.value?.index ?: -1,
                children0 = children[0],
                children1 = children[1],
                children2 = children[2],
                children3 = children[3],
            )
        }

        @JvmStatic
        private fun Long.bitAt(offset: Int): Boolean = this and (1L shl offset) != 0L

        @JvmStatic
        private fun Long.setBit(offset: Int): Long = this or (1L shl offset)
    }
}
