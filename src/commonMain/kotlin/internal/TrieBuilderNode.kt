// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal

/** A node of a Trie that is still in its building phase. */
internal class TrieBuilderNode(
    private val parent: TrieBuilderNode?,
    private val via: Char,
) {

    var index: Int = -1
    val depth: Int = parent?.depth?.plus(1) ?: 0
    var isMatch: Boolean = false
    var suffixNode: TrieBuilderNode? = null

    @Suppress("LateinitUsage")
    lateinit var failNode: TrieBuilderNode
    val children: MutableMap<Char, TrieBuilderNode> = mutableMapOf()

    /** Calculate the fail and suffix nodes. Should only be called after all nodes of lesser depth were computed. */
    fun compute() {
        if (parent == null) {
            failNode = this
            return
        }
        failNode = parent.failNode.goTo(via)
        var suffixSearcher = failNode
        while (suffixSearcher.parent != null && !suffixSearcher.isMatch) {
            suffixSearcher = suffixSearcher.failNode
        }
        if (suffixSearcher.isMatch) {
            suffixNode = suffixSearcher
        }
    }

    /** Construct the proper final node. */
    fun build(): TrieNode = TrieNodeImpl(
        index = index,
        depthAndMatch = depth * if (isMatch) 1 else -1,
        suffixNode = suffixNode?.index ?: -1,
        failNode = failNode.index,
        children = children.mapValues { (_, v) -> v.index },
    )

    /** Calculate the next node that should be visited if receiving [input]. */
    private fun goTo(input: Char): TrieBuilderNode {
        var dest = this
        while (dest.parent != null && !dest.children.containsKey(input)) {
            dest = dest.failNode
        }
        return dest.children.getOrElse(input) { dest.failNode }
    }
}
