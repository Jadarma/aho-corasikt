// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.trie

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.internal.nodes.TrieNodes
import io.github.jadarma.ahocorasikt.internal.util.Remapper

internal class PayloadTrieImpl<T : Any>(
    overlap: Overlap,
    matchType: MatchType,
    caseSensitivity: CaseSensitivity,
    remapper: Remapper,
    nodes: TrieNodes,
    private val payloads: ArrayList<T>,
    private val outputs: Map<Int, Int>,
) : TrieImpl(overlap, matchType, caseSensitivity, remapper, nodes), PayloadTrie<T> {

    override fun contains(payload: T): Boolean = false

    override fun searchWithPayload(
        text: CharSequence,
        requireNoOverlaps: Boolean
    ): Sequence<Pair<IntRange, T>> {
        val candidates = mutableMapOf<IntRange, Pair<IntRange, T>>()

        return search(
            text = text,
            requireNoOverlaps = requireNoOverlaps,
            onOffer = { range, node -> candidates[range] = range to payloads[outputs.getValue(node)] },
            onConfirm = { index, ranges ->
                for (range in ranges) {
                    yield(candidates.remove(range)!!)
                }
                candidates.entries.removeAll { it.key.last < index - depth }
            }
        )
    }
}
