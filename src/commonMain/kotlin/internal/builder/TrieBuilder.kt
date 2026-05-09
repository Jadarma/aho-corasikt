// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.builder

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.trie.TrieImpl
import io.github.jadarma.ahocorasikt.internal.asFunction
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNode.Companion.MAX_COMPACT_SIZE
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.DefaultTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.RemappedCompactTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.TrieNodes
import io.github.jadarma.ahocorasikt.internal.trie.PayloadTrieImpl

internal class TrieBuilder<T: Any> internal constructor(
    private val caseSensitivity: CaseSensitivity,
    private val matchType: MatchType,
    private val overlap: Overlap,
) : Trie.Builder, PayloadTrie.Builder<T> {

    private var built = false

    /** Internal flag used only for testing. All production Tries are optimized. */
    internal var optimizationLevel: Int = Int.MAX_VALUE

    private val root = TrieBuilderNode(null, null)
    private var size = 1

    private val alphabet = mutableSetOf<Int>()
    private val payloads = LinkedHashMap<T, Int>()

    private var caseMapping = caseSensitivity.asFunction()

    private fun addInternal(word: CharSequence, payload: T?) {
        check(!built) { "TrieBuilder already consumed." }
        require(word.isNotEmpty()) { "Word cannot be empty." }

        if(payload != null && payload !in payloads) {
            payloads[payload] = payloads.size
        }

        word
            .fold(root) { node, char ->
                val c = caseMapping(char).code
                alphabet.add(c)
                node.children.getOrPut(c) {
                    size++
                    TrieBuilderNode(parent = node, payload)
                }
            }
            .apply { isMatch = true }
    }

    override fun add(word: CharSequence): Trie.Builder = apply { addInternal(word, null) }

    override fun add(word: CharSequence, payload: T): PayloadTrie.Builder<T> = apply { addInternal(word, payload) }

    @Suppress("UNCHECKED_CAST")
    override fun build(): PayloadTrie<T> {
        check(!built) { "TrieBuilder already consumed." }
        require(size > 1) { "No keywords added, nothing to build." }
        built = true

        val nodes = arrayOfNulls<TrieBuilderNode>(size)
        val outputs = mutableMapOf<Int, Int>()
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
            if(node.payload != null) {
                outputs[node.index] = payloads.getValue(node.payload as T)
            }
            for((input, child) in node.children.entries.sortedBy { it.key }) {
                child.index = tail
                child.compute(via = input)
                nodes[tail++] = child
            }
        }

        check(head == tail && tail == size) { "Did not traverse all trie nodes." }
        val trieNodes = optimize(nodes as Array<TrieBuilderNode>)

        return if(outputs.isNotEmpty()) {
            PayloadTrieImpl(
                overlap = overlap,
                matchType = matchType,
                caseSensitivity = caseSensitivity,
                nodes = trieNodes,
                payloads = ArrayList<T>(outputs.size).apply { addAll(payloads.keys) },
                outputs = outputs
            )
        } else {
            TrieImpl(overlap, matchType, caseSensitivity, trieNodes) as PayloadTrie<T>
        }
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
