// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal

import kotlin.math.absoluteValue

/** A node of an already finalized trie. */
internal interface TrieNode {

    /**
     * The index of this node when iterating the trie in breadth-first-order.
     * The root note has index 0, and this value is never negative.
     */
    val index: Int

    /**
     * The depth of this node in the trie, with the root having depth 0.
     * This value also coincides with the length of the match, if applicable.
     * This value is never negative.
     */
    val depth: Int

    /** Whether this node is a match state _(i.e.: a dictionary word has been found when reaching this state)_. */
    val isMatch: Boolean

    /**
     * If positive, provides the index to the longest suffix node in the trie.
     * In order to find all occurrences, the suffix node should be followed recursively until a non-positive value is
     * found.
     */
    val suffixNode: Int

    /** The index of this node's fail case, or where to look if there is no associated child node for a given input. */
    val failNode: Int

    /** A mapping from input characters to the index of their associated nodes. */
    val children: Map<Char, Int>
}

internal data class TrieNodeImpl(
    override val index: Int,
    val depthAndMatch: Int,
    override val suffixNode: Int,
    override val failNode: Int,
    override val children: Map<Char, Int>,
) : TrieNode {
    override val depth: Int get() = depthAndMatch.absoluteValue
    override val isMatch: Boolean get() = depthAndMatch > 0
}
