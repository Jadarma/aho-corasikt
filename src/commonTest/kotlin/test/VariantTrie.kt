package io.github.jadarma.ahocorasikt.test

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.CaseSensitivity.Sensitive
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.MatchType.Unrestricted
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Overlap.Allowed
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilder
import io.github.jadarma.ahocorasikt.internal.nodes.CompactTrieNode.Companion.MAX_COMPACT_SIZE
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAllKeys
import io.kotest.matchers.shouldBe

/**
 * Used for testing to ensure the optimization methods hold.
 * It implements the [Trie] interface and ensures that all [optimizedVariants] produce the exact same output as
 * the [default] one.
 * Use [Trie.Companion.ofVariants] to construct a variant Trie for any inputs.
 *
 * @param default           The reference implementation, assumed correct.
 * @param optimizedVariants Variations of the same trie build with different optimizations.
 */
class VariantTrie(
    private val default: Trie,
    private val optimizedVariants: Map<String, Trie>,
) : Trie {

    init {
        require(optimizedVariants.isNotEmpty()) { "Don't use a VariantTrie without any variants."}

        withClue("Optimised variant(s) must use the same settings.") {
            optimizedVariants.values.apply {
                forAll { it.caseSensitivity shouldBe default.caseSensitivity }
                forAll { it.matchType shouldBe default.matchType }
                forAll { it.overlap shouldBe default.overlap }
                forAll { it.size shouldBe default.size }
                forAll { it.depth shouldBe default.depth }
            }
        }
    }

    override val caseSensitivity: CaseSensitivity get() = default.caseSensitivity
    override val matchType: MatchType get() = default.matchType
    override val overlap: Overlap get() = default.overlap
    override val depth: Int get() = default.depth
    override val size: Int get() = default.size

    override fun search(
        text: CharSequence,
        requireNoOverlaps: Boolean
    ): Sequence<IntRange> {
        return sequence {
            val defaultIter = default.search(text, requireNoOverlaps).iterator()
            val optimizedIters = optimizedVariants.mapValues { it.value.search(text, requireNoOverlaps).iterator() }

            while (true) {
                val defaultHasNext = defaultIter.hasNext()
                optimizedIters.forAllKeys { variant ->
                    withClue("The $variant variant doesn't have the same match count as default implementation.") {
                        optimizedIters.getValue(variant).hasNext() shouldBe defaultHasNext
                    }
                }

                if (!defaultHasNext) break
                val defaultNext = defaultIter.next()
                optimizedIters.forAllKeys { variant ->
                    withClue("The $variant variant did not produce the same next match as default implementation.") {
                        optimizedIters.getValue(variant).next() shouldBe defaultNext
                    }
                }

                yield(defaultNext)
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
    keywords: Collection<CharSequence>,
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
): Trie {
    val alphabet = mutableSetOf<Char>()
    for (word in keywords) alphabet += word.toSet()
    val canCompact = alphabet.size <= MAX_COMPACT_SIZE
    val needsRemapping = alphabet.maxOf(Char::code) >= MAX_COMPACT_SIZE

    fun buildWithLevel(level: Int): Trie = Trie
        .builder(caseSensitivity, matchType, overlap)
        .addAll(keywords)
        .apply { (this as TrieBuilder<*>).optimizationLevel = level }
        .build()

    val default = buildWithLevel(0)
    val variants = buildMap {
        if (canCompact && !needsRemapping) put("Compact", buildWithLevel(1))
        if (canCompact && needsRemapping) put("Remapped", buildWithLevel(2))
    }

    return if(variants.isEmpty()) default
    else VariantTrie(default, variants)
}
