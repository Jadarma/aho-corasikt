// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.nodes

/**
 * Abstracts the trie data structure and offers a low-level API to traverse it as a state machine.
 * Implementations should optimize for memory and access speed.
 */
internal sealed interface TrieNodes {

    /**
     * The number of nodes in this trie, including the root.
     * All calls to this class must use indexes in `0..<size` range.
     */
    val size: Int

    /** The maximum depth of any node. */
    val depth: Int

    /** Whether the node at the given [index] is an output node. */
    fun isMatch(index: Int): Boolean

    /**
     * Returns the depth in the trie of the node at the given [index].
     * The depth of the root node is `0`.
     */
    fun depthOf(index: Int): Int

    /** Returns the index of this node's suffix link, or `-1` if it does not have one. */
    fun suffixOf(index: Int): Int

    /**
     * Calculates the next position in the state machine.
     *
     * @param index The current state index.
     * @param input The next value in the haystack _(e.g.: value of next character in searched text)_
     *
     * @return The index of the next state.
     *         This function automatically handles traversing fail nodes.
     */
    fun next(index: Int, input: Int): Int
}
