// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.builder

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.TrieImpl
import io.github.jadarma.ahocorasikt.internal.asFunction
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNode.Companion.MAX_COMPACT_SIZE
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.DefaultTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.RemappedCompactTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.TrieNodes

internal class TrieBuilder internal constructor(
    private val caseSensitivity: CaseSensitivity,
    private val matchType: MatchType,
    private val overlap: Overlap,
) : Trie.Builder {

    private var built = false

    /** Internal flag used only for testing. All production Tries are optimized. */
    internal var optimizationLevel: Int = Int.MAX_VALUE

    private val root = TrieBuilderNode(null, -1)
    private var size = 1

    private val alphabet = mutableSetOf<Int>()

    private var caseMapping = caseSensitivity.asFunction()

    override fun add(word: CharSequence): TrieBuilder {
        check(!built) { "TrieBuilder already consumed." }
        require(word.isNotEmpty()) { "Word cannot be empty." }
        word
            .fold(root) { node, char ->
                val c = caseMapping(char).code
                alphabet.add(c)
                node.children.getOrPut(c) {
                    size++
                    TrieBuilderNode(parent = node, via = c)
                }
            }
            .apply { isMatch = true }

        return this
    }

    override fun build(): Trie {
        check(!built) { "TrieBuilder already consumed." }
        require(size > 1) { "No keywords added, nothing to build." }
        built = true

        val nodes = arrayOfNulls<TrieBuilderNode>(size)
        var head = 1
        var tail = 1

        nodes[0] = root.apply {
            index = 0
            failNode = root
            children.entries.sortedBy { it.key }.forEach { (_, child) ->
                child.index = tail
                child.failNode = this
                nodes[tail++] = child
            }
        }

        while(head < nodes.size) {
            val node = nodes[head++]!!
            for((_, child) in node.children.entries.sortedBy { it.key }) {
                child.index = tail
                child.compute()
                nodes[tail++] = child
            }
        }

        check(head == tail && tail == size) { "Did not traverse all trie nodes." }

        @Suppress("UNCHECKED_CAST")
        val trieNodes = optimize(nodes as Array<TrieBuilderNode>)
        return TrieImpl(overlap, matchType, caseSensitivity, trieNodes)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun optimize(original: Array<TrieBuilderNode>): TrieNodes {
        val needsRemapping = alphabet.size > MAX_COMPACT_SIZE
        val canCompact = alphabet.size <= MAX_COMPACT_SIZE

        val optLevel = when (optimizationLevel) {
            0 -> 0
            1 -> check(canCompact && !needsRemapping).let { 1 }
            2 -> check(canCompact).let { 2 }
            else -> when {
                !canCompact -> 0
                !needsRemapping -> 1
                needsRemapping -> 2
                else -> 0
            }
        }

        val remapping = alphabet
            .takeIf { needsRemapping || optimizationLevel == 2 }.orEmpty()
            .sorted().withIndex()
            .associate { it.value to it.index }

        return when (optLevel) {
            0 -> DefaultTrieNodes.from(original)
            1 -> CompactTrieNodes.from(original)
            2 -> RemappedCompactTrieNodes.from(original, remapping)
            else -> error("Invalid optimization setting.")
        }
    }
}
