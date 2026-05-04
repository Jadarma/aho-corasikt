// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.nodes

import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

/**
 * Stores nodes in a breadth-first-ordered array of nodes.
 * The children of nodes are stored as index association maps.
 * This should be used when the trie is very dense, or requires children of large indexes.
 */
@JvmInline
internal value class HashMapNodes(val nodes: Array<TrieHashNode>) : TrieNodes {

    override val size: Int get() = nodes.size

    override val depth: Int get() = nodes.last().depth

    override fun isMatch(index: Int): Boolean = nodes[index].isMatch

    override fun depthOf(index: Int): Int = nodes[index].depth

    override fun suffixOf(index: Int): Int = nodes[index].suffixNode

    override fun next(index: Int, input: Int): Int {
        var dest: TrieHashNode = nodes[index]
        while (dest.index != 0 && !dest.children.containsKey(input)) {
            dest = nodes[dest.failNode]
        }
        return dest.children[input] ?: dest.failNode
    }
}

internal class TrieHashNode(
    val index: Int,
    val depthAndMatch: Int,
    val suffixNode: Int,
    val failNode: Int,
    val children: Map<Int, Int>,
) {
    val depth: Int get() = depthAndMatch.absoluteValue
    val isMatch: Boolean get() = depthAndMatch > 0
}
