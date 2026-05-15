// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.trie

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.nodes.TrieNodes

internal class TrieImpl(
    overlap: Overlap,
    matchType: MatchType,
    caseSensitivity: CaseSensitivity,
    nodes: TrieNodes,
) : AbstractTrie(overlap, matchType, caseSensitivity, nodes), Trie, PayloadTrie<Nothing> {

    override fun contains(payload: Nothing) =
        throw UnsupportedOperationException("This trie was not built with payload supports.")

    override fun searchWithPayload(text: CharSequence, requireNoOverlaps: Boolean) =
        throw UnsupportedOperationException("This trie was not built with payload supports.")
}
