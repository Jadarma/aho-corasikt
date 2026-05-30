package io.github.jadarma.ahocorasikt.test

import io.github.jadarma.ahocorasikt.*
import io.github.jadarma.ahocorasikt.CaseSensitivity.Sensitive
import io.github.jadarma.ahocorasikt.MatchType.Unrestricted
import io.github.jadarma.ahocorasikt.Overlap.Allowed
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase.TrieVariant
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNode.Companion.MAX_COMPACT_SIZE
import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAllKeys
import io.kotest.matchers.shouldBe

/**
 * Used for testing to ensure the optimization methods hold.
 * It implements the [Trie] interface and ensures that all variants produce the exact same output.
 * Use [Trie.Companion.ofVariants] to construct a variant Trie for any inputs.
 *
 * @property variants Variations of the same trie build with different optimizations.
 *                 It must contain an [TrieVariant.Unoptimized] variant, used as a reference.
 */
internal class VariantTrie private constructor(val variants: Map<TrieVariant, Trie>) : Trie {

    val reference: Trie

    init {
        require(variants.isNotEmpty()) { "Don't use a VariantTrie without any variants." }
        require(TrieVariant.Unoptimized in variants) { "Unoptimized variants are not supported." }
        reference = variants.getValue(TrieVariant.Unoptimized)
        withClue("All variants must use the same settings.") {
            variants.forAllKeys { variant ->
                val trie = variants.getValue(variant)
                trie.caseSensitivity shouldBe reference.caseSensitivity
                trie.matchType shouldBe reference.matchType
                trie.overlap shouldBe reference.overlap
                trie.size shouldBe reference.size
                trie.depth shouldBe reference.depth
            }
        }
    }

    override val caseSensitivity: CaseSensitivity get() = reference.caseSensitivity
    override val matchType: MatchType get() = reference.matchType
    override val overlap: Overlap get() = reference.overlap
    override val depth: Int get() = reference.depth
    override val size: Int get() = reference.size

    override fun search(
        text: CharSequence,
        requireNoOverlaps: Boolean
    ): Sequence<IntRange> = sequence {
        val defaultIter = reference.search(text, requireNoOverlaps).iterator()
        val variantIter = variants
            .filterValues { it !== reference }
            .mapValues { it.value.search(text, requireNoOverlaps).iterator() }

        var matchIndex = 0
        while (defaultIter.hasNext()) {
            withClue({ "Match #${matchIndex++}:" }) {
                val expectedMatch = defaultIter.next()
                variantIter.forAllKeys { variant ->
                    val iter = variantIter.getValue(variant)
                    if (!iter.hasNext()) {
                        fail("Expected to find $expectedMatch, but trie stopped.")
                    }
                    withClue({ "Trie match not the next expected." }) {
                        iter.next() shouldBe expectedMatch
                    }
                }
                yield(expectedMatch)
            }
        }
        variantIter.forAllKeys { variant ->
            val iter = variantIter.getValue(variant)
            if (iter.hasNext()) {
                fail("This variant found extra matches, first being ${iter.next()}")
            }
        }
    }

    companion object {

        fun of(
            keywords: Collection<CharSequence>,
            caseSensitivity: CaseSensitivity = Sensitive,
            matchType: MatchType = Unrestricted,
            overlap: Overlap = Allowed,
        ): Trie =
            possibleVariants(keywords)
                .associateWith { variant -> buildVariant(variant, keywords, caseSensitivity, matchType, overlap) }
                .let(::VariantTrie)

        fun buildVariant(
            variant: TrieVariant,
            keywords: Collection<CharSequence>,
            caseSensitivity: CaseSensitivity = Sensitive,
            matchType: MatchType = Unrestricted,
            overlap: Overlap = Allowed,
        ): Trie = buildTrie(caseSensitivity, matchType, overlap) {
            (this as TrieBuilderBase<*>).forceVariant = variant
            addAll(keywords)
        }

        /** For a set of keywords, determine possible optimization variants for a trie. */
        private fun possibleVariants(keywords: Collection<CharSequence>): Set<TrieVariant> = buildSet {
            val alphabet = buildSet(capacity = 1024) { for (word in keywords) addAll(word.asIterable()) }
            val canCompact = alphabet.size <= MAX_COMPACT_SIZE

            addAll(TrieVariant.entries)
            if (!canCompact) remove(TrieVariant.Compact)
        }
    }
}

/**
 * To be used in testing instead of normal builder.
 * Will instead evaluate if the [keywords] would be able to build an optimized trie.
 * If so, returns a [VariantTrie] instead, otherwise delegates to original implementation.
 */
fun Trie.Companion.ofVariants(
    keywords: Collection<CharSequence>,
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
): Trie = VariantTrie.of(keywords, caseSensitivity, matchType, overlap)
