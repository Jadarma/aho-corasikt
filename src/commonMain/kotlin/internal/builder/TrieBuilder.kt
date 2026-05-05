// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.builder

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.TrieImpl
import io.github.jadarma.ahocorasikt.internal.asFunction
import io.github.jadarma.ahocorasikt.internal.nodes.CompactedNodes
import io.github.jadarma.ahocorasikt.internal.nodes.TrieCompactNode
import io.github.jadarma.ahocorasikt.internal.nodes.HashMapNodes
import io.github.jadarma.ahocorasikt.internal.nodes.TrieCompactNode.Companion.MAX_COMPACT_SIZE
import io.github.jadarma.ahocorasikt.internal.nodes.TrieHashNode

internal class TrieBuilder internal constructor(
    private val caseSensitivity: CaseSensitivity,
    private val matchType: MatchType,
    private val overlap: Overlap,
) : Trie.Builder {

    private var built = false

    /** Internal flag used only for testing. All production Tries are optimized. */
    internal var disableOptimization = false

    private var canOptimize = true

    private val root = TrieBuilderNode(null, -1)
    private var size = 1

    private var caseMapping = caseSensitivity.asFunction()

    override fun add(word: CharSequence): TrieBuilder {
        check(!built) { "TrieBuilder already consumed." }
        require(word.isNotEmpty()) { "Word cannot be empty." }
        word
            .fold(root) { node, char ->
                val c = caseMapping(char).code
                if(c >= MAX_COMPACT_SIZE) canOptimize = false
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
        built = true
        return if (!disableOptimization && canOptimize) buildCompact() else buildNormal()
    }

    private fun buildNormal(): Trie {
        val queue = ArrayDeque<TrieBuilderNode>()
        val trieNodes = arrayOfNulls<TrieHashNode>(size)
        var counter = 0
        var inserted = 0

        root.index = 0
        root.failNode = root
        root.children.entries.sortedBy { it.key }.forEach { (_, child) ->
            child.index = ++counter
            child.failNode = root
            queue.addLast(child)
        }
        trieNodes[0] = TrieHashNode.from(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for ((_, child) in node.children.entries.sortedBy { it.key }) {
                child.index = ++counter
                child.compute()
                queue.addLast(child)
            }
            trieNodes[++inserted] = TrieHashNode.from(node)
        }

        check(inserted == counter && counter == size - 1) { "Did not traverse all trie nodes." }

        @Suppress("UNCHECKED_CAST")
        val nodes = HashMapNodes(trieNodes as Array<TrieHashNode>)

        return TrieImpl(overlap, matchType, caseSensitivity, nodes)
    }

    private fun buildCompact(): Trie {
        val queue = ArrayDeque<TrieBuilderNode>()
        val trieNodes = arrayOfNulls<TrieCompactNode>(size)
        var counter = 0
        var inserted = 0

        root.index = 0
        root.failNode = root
        root.children.entries.sortedBy { it.key }.forEach { (_, child) ->
            child.index = ++counter
            child.failNode = root
            queue.addLast(child)
        }
        trieNodes[0] = TrieCompactNode.from(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for ((_, child) in node.children.entries.sortedBy { it.key }) {
                child.index = ++counter
                child.compute()
                queue.addLast(child)
            }
            trieNodes[++inserted] = TrieCompactNode.from(node)
        }

        check(inserted == counter && counter == size - 1) { "Did not traverse all trie nodes." }

        @Suppress("UNCHECKED_CAST")
        val nodes = CompactedNodes(trieNodes as Array<TrieCompactNode>)

        return TrieImpl(overlap, matchType, caseSensitivity, nodes)
    }
}
