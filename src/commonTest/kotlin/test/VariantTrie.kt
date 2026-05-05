package io.github.jadarma.ahocorasikt.test

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.CaseSensitivity.Sensitive
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.MatchType.Unrestricted
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Overlap.Allowed
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.Trie.Companion.builder
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilder
import io.github.jadarma.ahocorasikt.internal.nodes.TrieCompactNode
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A variant trie holds two variants internally:
 * - The unoptimized one, the fallback used for complex Unicode patterns.
 * - The optimized one, that uses ASCII-only or few unique inputs, and has faster internals.
 *
 * It implements the [Trie] interface and ensures that both of them produce the exact same results.
 * Used for testing to ensure the unoptimized variant works and is valid, even without crafting special input so the
 * default builder would fall back to using it.
 */
class VariantTrie(
    private val unoptimised: Trie,
    private val optimised: Trie,
) : Trie {

    init {
        withClue("Optimised variant must use the same settings.") {
            optimised.caseSensitivity shouldBe unoptimised.caseSensitivity
            optimised.matchType shouldBe unoptimised.matchType
            optimised.overlap shouldBe unoptimised.overlap
            optimised.size shouldBe unoptimised.size
            optimised.depth shouldBe unoptimised.depth
        }
    }

    override val caseSensitivity: CaseSensitivity get() = unoptimised.caseSensitivity
    override val matchType: MatchType get() = unoptimised.matchType
    override val overlap: Overlap get() = unoptimised.overlap
    override val depth: Int get() = unoptimised.depth
    override val size: Int get() = unoptimised.size

    override fun search(
        text: CharSequence,
        requireNoOverlaps: Boolean
    ): Sequence<IntRange> {
        return sequence {
            val optimisedIter = optimised.search(text, requireNoOverlaps).iterator()
            val unoptimisedIter = unoptimised.search(text, requireNoOverlaps).iterator()

            while (true) {
                val optimisedHasNext = optimisedIter.hasNext()
                val unoptimisedHasNext = unoptimisedIter.hasNext()
                check(optimisedHasNext == unoptimisedHasNext) {
                    "Variants differ on hasNext(): unoptimised = $unoptimisedHasNext, optimised = $optimisedHasNext"
                }
                if (!optimisedHasNext) break

                val optimisedNext = optimisedIter.next()
                val unoptimisedNext = unoptimisedIter.next()
                check(optimisedNext == unoptimisedNext) {
                    "Variants differ on next(): unoptimised = $unoptimisedNext, optimised = $optimisedNext"
                }

                yield(optimisedNext)
            }
        }
    }
}

/**
 * To be used in testing instead of [Trie.of].
 * Will instead evaluate if the [keywords] would be able to build an optimized trie.
 * If so, returns a [VariantTrie] instead, otherwise delegates to original implementation.
 */
fun Trie.Companion.ofVariants(
    keywords: Iterable<CharSequence>,
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
): Trie {
    var optimizationPossible = true
    val uniqueInputs = mutableSetOf<Char>()
    for (input in keywords) {
        if (!optimizationPossible) continue
        for (char in input) {
            if (uniqueInputs.size < TrieCompactNode.MAX_COMPACT_SIZE) {
                uniqueInputs.add(char)
                continue
            }
            if (char.code >= TrieCompactNode.MAX_COMPACT_SIZE) {
                optimizationPossible = false
                break
            }
        }
    }

    // If optimization is not possible, just return the normal variant.
    if (!optimizationPossible) return Trie.of(keywords, caseSensitivity, matchType, overlap)

    return VariantTrie(
        unoptimised = builder(caseSensitivity, matchType, overlap)
            .addAll(keywords)
            .apply { (this as TrieBuilder).disableOptimization = true }
            .build(),
        optimised = builder(caseSensitivity, matchType, overlap)
            .addAll(keywords)
            .build(),
    )
}
