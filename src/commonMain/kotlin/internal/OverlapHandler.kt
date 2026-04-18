// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal

import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Overlap.Ignored
import io.github.jadarma.ahocorasikt.Overlap.Leftmost
import io.github.jadarma.ahocorasikt.Overlap.Longest
import io.github.jadarma.ahocorasikt.Trie

/**
 * Collects the emitted matches of a trie, and buffers them until it is guaranteed that the ranges may never overlap.
 *
 * During a search, matches of the trie should be passed to [offer] instead of yielded directly.
 * The [check] and [checkAndFinalize] functions should be used to retrieve the validated ranges.
 *
 * @param buffer The size of the sliding buffer to hold ranges from.
 *               This value should be set to the [depth][Trie.depth] of the trie this handler is used for, based on the
 *               observation that any match would be at most depth characters, making overlapping outside of it
 *               impossible.
 */
internal sealed class Overlapper(private val buffer: Int) {

    /** These ranges have already been validated and are ready to be yielded as search results. */
    private val validated: ArrayDeque<IntRange> = ArrayDeque(INITIAL_CAPACITY)

    /** These ranges are matches in the trie but may or may not be discarded in favor of matches not yet encountered. */
    protected val candidates: ArrayDeque<IntRange> = ArrayDeque(INITIAL_CAPACITY)

    /**
     * Offer this [range] for checking.
     * It shall be tested and possibly kept as a candidate while in the buffer zone.
     * Also advances the buffer's sliding window and promotes any candidates outside it to validated matches.
     *
     * **NOTE:** This ***assumes*** that the end of the given [range] is greater than or equal to any previously offered
     *           ranges, because that is an invariant of the trie matching algorithm.
     */
    fun offer(range: IntRange) {
        promoteCandidates(range.last)
        offerCandidate(range)
    }

    /**
     * Returns all the validated ranges that can be yielded.
     * It is given the [index] of the current search to determine if the buffered candidates can also be validated now.
     */
    fun check(index: Int): List<IntRange> {
        promoteCandidates(index)
        return consumeValidated()
    }

    /**
     * An edge case of [check], to be called when reaching the end of the input text.
     * Since no more matches can occur, promotes all candidates to validated ranges.
     * This is a terminal operation, and this handler should not be used anymore after calling this function.
     */
    fun checkAndFinalize(): List<IntRange> {
        filterCandidates()
        validated.addAll(candidates)
        return validated
    }

    /**
     * Called to check whether the buffer zone has been exceeded.
     * If it did, finalizes by calling [filterCandidates], then transfers all remaining ranges to the [validated]
     * queue.
     */
    private fun promoteCandidates(index: Int) {
        val lastEnd = candidates.lastOrNull()?.last
        if (lastEnd == null || index - buffer < lastEnd) return
        filterCandidates()
        validated.addAll(candidates)
        candidates.clear()
    }

    /** Returns a copy of the validated ranges that can be safely emitted, and clears the internal buffer. */
    private fun consumeValidated(): List<IntRange> {
        val ranges = validated.toList()
        validated.clear()
        return ranges
    }

    open fun offerCandidate(range: IntRange) {
        candidates.add(range)
    }

    open fun filterCandidates() = Unit

    companion object {

        private const val INITIAL_CAPACITY: Int = 10

        /** Get a new instance of an [Overlapper] for the given [strategy] and [trieDepth]. */
        fun of(strategy: Overlap, trieDepth: Int): Overlapper = when (strategy) {
            Ignored -> IgnoredOverlapper()
            Leftmost -> LeftmostOverlapper(trieDepth)
            Longest -> LongestOverlapper(trieDepth)
        }

        /** Returns `true` if these two ranges overlap on at least one index. */
        @JvmStatic
        protected infix fun IntRange.overlaps(other: IntRange): Boolean =
            this.first <= other.last && this.last >= other.first
    }
}

/** Implements [Overlap.Ignored] strategy; effectively a NOOP handler. */
internal class IgnoredOverlapper : Overlapper(buffer = 0)

/** Implements [Overlap.Leftmost] strategy. */
internal class LeftmostOverlapper(maxDepth: Int) : Overlapper(buffer = maxDepth) {
    override fun offerCandidate(range: IntRange) {
        val eliminatedByExisting = candidates.any { range overlaps it && it.first < range.first }
        if (eliminatedByExisting) return
        candidates.removeAll { range overlaps it }
        candidates.addLast(range)
    }
}

/** Implements [Overlap.Longest] strategy. */
internal class LongestOverlapper(maxDepth: Int) : Overlapper(buffer = maxDepth) {

    override fun filterCandidates() {
        candidates.sortWith(
            compareByDescending<IntRange> { maxOf(1, it.last - it.first) }
                .thenByDescending { it.first }
        )
        var domIndex = 0
        while(domIndex < candidates.lastIndex) {
            val dominant = candidates[domIndex]
            var subIndex = domIndex + 1
            while(subIndex <= candidates.lastIndex) {
                if(dominant overlaps candidates[subIndex]) {
                    candidates.removeAt(subIndex)
                } else {
                    subIndex++
                }
            }
            domIndex++
        }
        candidates.sortWith(compareBy(IntRange::last).thenBy(IntRange::first))
    }
}
