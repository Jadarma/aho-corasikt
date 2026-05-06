// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderNode
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import kotlin.math.absoluteValue

/**
 * Stores nodes in a breadth-first-ordered array.
 * The children of nodes are not stored directly, rather each node remembers the index of the first child node, and
 * has a bitset, where the _n_-th bit is set if a connection exists to a child node via _n_.
 * The actual index of the child is calculated by counting how many bits are set to left of n, and adding that to the
 * index of the first child.
 *
 * This optimization improves performance as there are no hashmap calculations to maintain, and the node itself is
 * contiguous in memory, reducing memory lookup.
 * However, it may only be used when all inputs are in `0..255` range.
 */
@JvmInline
internal value class CompactedNodes(val nodes: Array<TrieCompactNode>) : TrieNodes {

    override val size: Int get() = nodes.size

    override val depth: Int get() = nodes.last().depth

    override fun isMatch(index: Int): Boolean = nodes[index].isMatch

    override fun depthOf(index: Int): Int = nodes[index].depth

    override fun suffixOf(index: Int): Int = nodes[index].suffixNode

    override fun next(index: Int, input: Int): Int {
        var dest: TrieCompactNode = nodes[index]
        while (dest.depthAndMatch != 0 && input !in dest) {
            dest = nodes[dest.failNode]
        }
        return dest.next(input)
    }
}

@Suppress("MagicNumber")
internal class TrieCompactNode(
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

    operator fun contains(input: Int): Boolean {
        val bucket = when (input.ushr(6)) {
            0 -> children0
            1 -> children1
            2 -> children2
            3 -> children3
            else -> return false
        }
        val bucketOffset = 63 - input.rem(64)
        return bucket.bitAt(bucketOffset)
    }

    fun next(input: Int): Int {
        val bucketIndex = input.ushr(6)
        val bucket = when (bucketIndex) {
            0 -> children0
            1 -> children1
            2 -> children2
            3 -> children3
            else -> return failNode
        }
        val bucketOffset = 63 - input.rem(64)
        if (!bucket.bitAt(bucketOffset)) return failNode

        var offset = childOffset
        if (bucketIndex > 0) offset += children0.countOneBits()
        if (bucketIndex > 1) offset += children1.countOneBits()
        if (bucketIndex > 2) offset += children2.countOneBits()
        if (bucketOffset < 63) offset += (bucket ushr bucketOffset.inc()).countOneBits()
        return offset
    }

    companion object {

        /**
         * The maximum number of children a [TrieCompactNode] can have.
         * Values of all child connections (input value transitions) must also be lower than this value.
         */
        const val MAX_COMPACT_SIZE = 256

        fun from(node: TrieBuilderNode): TrieCompactNode {
            val children = LongArray(4).apply {
                for (input in node.children.keys.sorted()) {
                    val bucket = input.ushr(6)
                    val bucketOffset = 63 - input.rem(64)
                    this[bucket] = this[bucket].setBit(bucketOffset)
                }
            }
            return TrieCompactNode(
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
