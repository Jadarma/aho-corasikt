// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.builder

/** A node of a Trie that is still in its building phase. */
internal class TrieBuilderNode(
    private val parent: TrieBuilderNode?,
    private val via: Int,
) {

    var index: Int = -1
    val depth: Int = parent?.depth?.plus(1) ?: 0
    var isMatch: Boolean = false
    var suffixNode: TrieBuilderNode? = null

    @Suppress("LateinitUsage")
    lateinit var failNode: TrieBuilderNode

    val children: MutableMap<Int, TrieBuilderNode> = mutableMapOf()

    /** Calculate the fail and suffix nodes. Should only be called after all nodes of lesser depth were computed. */
    fun compute() {
        failNode = parent!!.failNode.next(via)
        var suffix = failNode
        while (!suffix.isMatch && suffix.depth > 0) suffix = suffix.failNode
        if (suffix.isMatch) suffixNode = suffix
    }

    /** Calculate the next node that should be visited if receiving [input]. */
    private fun next(input: Int): TrieBuilderNode {
        var dest = this
        while (!dest.children.containsKey(input) && dest.depth > 0) dest = dest.failNode
        return dest.children.getOrElse(input) { dest.failNode }
    }
}
