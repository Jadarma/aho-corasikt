// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderNode
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

/**
 * Stores nodes in a breadth-first-ordered array of nodes.
 * The children of nodes are stored as index association maps.
 * Due to the nodes making use of hashmaps and spread across allocated memory, it is less efficient.
 * This should be used as a fallback when other optimizations cannot be used.
 */
@JvmInline
internal value class DefaultTrieNodes(val nodes: Array<DefaultTrieNode>) : TrieNodes {

    override val size: Int get() = nodes.size

    override val depth: Int get() = nodes.last().depth

    override fun isMatch(index: Int): Boolean = nodes[index].isMatch

    override fun depthOf(index: Int): Int = nodes[index].depth

    override fun suffixOf(index: Int): Int = nodes[index].suffixNode

    override fun next(index: Int, input: Int): Int {
        var dest: DefaultTrieNode = nodes[index]
        while (dest.depthAndMatch != 0 && !dest.children.containsKey(input)) {
            dest = nodes[dest.failNode]
        }
        return dest.children[input] ?: dest.failNode
    }

    companion object {

        fun from(original: Array<TrieBuilderNode>): DefaultTrieNodes {
            val nodes = Array(original.size) {
                val node = original[it]
                DefaultTrieNode(
                    depthAndMatch = node.depth * if (node.isMatch) 1 else -1,
                    suffixNode = node.suffixNode?.index ?: -1,
                    failNode = node.failNode.index,
                    children = node.children.entries.associate { (k, v) -> k to v.index },
                )
            }
            return DefaultTrieNodes(nodes)
        }
    }
}

internal class DefaultTrieNode(
    val depthAndMatch: Int,
    val suffixNode: Int,
    val failNode: Int,
    val children: Map<Int, Int>,
) {
    val depth: Int get() = depthAndMatch.absoluteValue
    val isMatch: Boolean get() = depthAndMatch > 0
}
