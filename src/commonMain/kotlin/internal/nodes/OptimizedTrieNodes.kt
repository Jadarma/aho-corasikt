// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.nodes

import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderNode
import io.github.jadarma.ahocorasikt.internal.util.Remapper
import io.github.jadarma.ahocorasikt.internal.util.Transitions
import kotlin.math.absoluteValue

/**
 * An optimized trie that uses a double array to encode transitions between nodes.
 * Technically, this is a *triple* array trie, since the [nodes] hold a reference to the `base` offset of the
 * transitions, which contain `next` and `check`, instead of being just a double array trie.
 * This is intentional, because a double array rearranges the nodes internally, whereas the triple array allows us to
 * keep the original indexes; without this the class couldn't be used as a drop-in replacement, because consumers of
 * this type expect stable indexes _(for example, when associating output states to payloads)_.
 */
internal class OptimizedTrieNodes(
    val nodes: Array<OptimizedTrieNode>,
    val transitions: LongArray,
) : TrieNodes {

    override val size: Int get() = nodes.size

    override val depth: Int get() = nodes.last().depth

    override fun isMatch(index: Int): Boolean = nodes[index].isMatch

    override fun depthOf(index: Int): Int = nodes[index].depth

    override fun suffixOf(index: Int): Int = nodes[index].suffix

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkSlot(slot: Int, test: Int): Boolean =
        slot >= 0 && slot < transitions.size && transitions[slot].toInt() == test

    @Suppress("NOTHING_TO_INLINE")
    private inline fun nextOf(slot: Int): Int =
        transitions[slot].ushr(Int.SIZE_BITS).toInt()

    override fun next(index: Int, input: Int): Int {
        if (input == Int.MAX_VALUE) return 0
        var node = index
        while (node != 0) {
            val slot = nodes[node].base + input
            if (checkSlot(slot, node)) return nextOf(slot)
            node = nodes[node].fail
        }
        val slot = nodes[0].base + input
        return if (checkSlot(slot, 0)) nextOf(slot) else 0
    }

    companion object {

        fun from(nodes: Array<TrieBuilderNode>, remap: Remapper): OptimizedTrieNodes {

            val transitions = Transitions(remap)
            val bases = IntArray(nodes.size)

            for (node in nodes) {
                val index = node.index
                bases[index] = transitions.insert(node)
            }

            return OptimizedTrieNodes(
                transitions = transitions.compact(),
                nodes = Array(nodes.size) { index ->
                    val node = nodes[index]
                    OptimizedTrieNode(
                        fail = node.failNode.index,
                        suffix = node.suffixNode?.index ?: -1,
                        depthAndMatch = node.depth * if (node.isMatch) 1 else -1,
                        base = bases[node.index],
                    )
                },
            )
        }
    }
}

internal class OptimizedTrieNode(
    val fail: Int,
    val suffix: Int,
    val depthAndMatch: Int,
    val base: Int,
) {
    val depth: Int get() = depthAndMatch.absoluteValue
    val isMatch: Boolean get() = depthAndMatch > 0
}
