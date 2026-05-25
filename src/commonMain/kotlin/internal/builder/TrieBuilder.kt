// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.builder

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.util.asFunction
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNode.Companion.MAX_COMPACT_SIZE
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.DefaultTrieNodes
import io.github.jadarma.ahocorasikt.internal.nodes.TrieNodes
import io.github.jadarma.ahocorasikt.internal.trie.PayloadTrieImpl
import io.github.jadarma.ahocorasikt.internal.trie.TrieImpl
import io.github.jadarma.ahocorasikt.internal.util.MapRemapper
import io.github.jadarma.ahocorasikt.internal.util.NoopRemapper
import io.github.jadarma.ahocorasikt.internal.util.Remapper
import kotlin.collections.set

internal sealed class TrieBuilderBase<T: Any>(
    val caseSensitivity: CaseSensitivity,
    val matchType: MatchType,
    val overlap: Overlap,
) {

    private var built = false

    /** Internal flag used only for testing. All production Tries are optimized. */
    internal var optimizationLevel: Int = Int.MAX_VALUE

    private val root = TrieBuilderNode(null, null)
    private var size = 1

    private val alphabet = mutableSetOf<Int>()
    private val caseMapping: (Char) -> Char = caseSensitivity.asFunction()

    protected val payloads = LinkedHashMap<T, Int>()
    protected val outputs = mutableMapOf<Int, Int>()

    protected fun addInternal(word: CharSequence, payload: T?) {
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

    @Suppress("UNCHECKED_CAST")
    protected fun buildInternal(): Pair<TrieNodes, Remapper> {
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
        return optimize(nodes as Array<TrieBuilderNode>)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun optimize(original: Array<TrieBuilderNode>): Pair<TrieNodes, Remapper> {
        val canCompact = alphabet.size <= MAX_COMPACT_SIZE
        val needsRemapping = alphabet.max() > MAX_COMPACT_SIZE

        val possibleOptimizationLevel = when {
            !canCompact -> 0
            needsRemapping -> 1
            !needsRemapping -> 2
            else -> 0
        }

        val optLevel = minOf(possibleOptimizationLevel, optimizationLevel)
        val remapper = if(needsRemapping || optLevel == 2) MapRemapper(alphabet) else NoopRemapper

        return when (optLevel) {
            0 -> DefaultTrieNodes.from(original) to NoopRemapper
            1 -> CompactTrieNodes.from(original, remapper) to remapper
            2 -> CompactTrieNodes.from(original, NoopRemapper) to NoopRemapper
            else -> error("Invalid optimization setting.")
        }
    }
}

internal class TrieBuilder(
    caseSensitivity: CaseSensitivity,
    matchType: MatchType,
    overlap: Overlap,
) : TrieBuilderBase<Nothing>(caseSensitivity, matchType, overlap), Trie.Builder {

    override fun add(word: CharSequence): Trie.Builder = apply { addInternal(word, null) }

    override fun build(): Trie {
        val (nodes, remapper) = buildInternal()
        return TrieImpl(overlap, matchType, caseSensitivity, remapper, nodes)
    }
}

internal class PayloadTrieBuilder<T : Any>(
    caseSensitivity: CaseSensitivity,
    matchType: MatchType,
    overlap: Overlap,
) : TrieBuilderBase<T>(caseSensitivity, matchType, overlap), PayloadTrie.Builder<T> {

    override fun add(word: CharSequence, payload: T): PayloadTrie.Builder<T> =
        apply { addInternal(word, payload) }

    override fun build(): PayloadTrie<T> {
        val (nodes, remapper) = buildInternal()
        return PayloadTrieImpl(
            overlap = overlap,
            matchType = matchType,
            caseSensitivity = caseSensitivity,
            remapper = remapper,
            nodes = nodes,
            payloads = ArrayList<T>(outputs.size).apply { addAll(payloads.keys) },
            outputs = outputs
        )
    }
}
