// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.overlap

import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Overlap.Ignored
import io.github.jadarma.ahocorasikt.Overlap.Leftmost
import io.github.jadarma.ahocorasikt.Overlap.Longest

/**
 * Collects the emitted matches of a trie, and buffers them until it is guaranteed that the ranges may never overlap.
 * If overlaps occur, removes them depending on implementation strategy.
 *
 * Uses a push and poll configuration: matching ranges are [offer]ed as they appear, then at regular intervals,
 * the [check] function is used to yield validated ranges.
 *
 * Invariants, assumptions, and contracts:
 * - The ranges are assumed to be [offer]ed in order of Aho-Corasick matching, i.e.: sorted ascendingly by endIndex and
 *   then by startIndex.
 * - The ranges yielded by [check] are consumed before the next range is [offer]ed.
 * - The [finalCheck] is always called once, and at the end of the search.
 */
internal sealed class Overlapper {

    /**
     * Holds known-good ranges that respect the overlap strategy.
     * Implementations can confirm a candidate using the [promote] and [promoteAll] functions.
     * The contents can be consumed externally via the [check] function.
     */
    private val validated = ArrayDeque<IntRange>(INITIAL_CAPACITY)

    /**
     * A little hack in order to not create a list copy of validated ranges.
     * The [check] function would need to consume the internal buffer and clear it, but also return it.
     * Instead, the [check] function returns the buffer as-is, but marks it as _dirty_.
     * If any new candidates are [promote]d, they should [flush] first to remove stale data.
     */
    private var flushPending = false

    /**
     * A candidate match occurred at the given [range].
     * Implementations may decide to promote, discard, or hold as a candidate for later referencing.
     */
    abstract fun offer(range: IntRange)

    /**
     * The caller indicates that the search is currently at the given [index].
     * The overlapper returns all ranges that are known to not overlap, and that will not ever overlap with any match
     * occurring after the [index].
     * The returned values ***must be immediately consumed*** before calling any other overlapper functions.
     */
    fun check(index: Int): List<IntRange> {
        checkInternal(index)
        flushPending = true
        return validated
    }

    /**
     * An edge case for [check], should be called when the end of the search is reached.
     * Since no more matches can occur, do any final checks on possibly pending candidates, and return them.
     * After invoking this function, the overlapper object should not be used anymore.
     */
    fun finalCheck(): List<IntRange> {
        finalizeInternal()
        return validated
    }

    protected open fun checkInternal(index: Int) = Unit

    protected open fun finalizeInternal() = Unit

    /** Confirm the [candidate] as a valid yield result. */
    protected fun promote(candidate: IntRange) {
        flush()
        validated.add(candidate)
    }

    /** Confirm the [candidates] as valid yield results. */
    protected fun promoteAll(candidates: List<IntRange>) {
        flush()
        validated.addAll(candidates)
    }

    /**
     * Called before any new validated ranges are inserted.
     * Check if the [validated] queue needs clearing after having been returned by [check].
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun flush() {
        if(flushPending) {
            validated.clear()
            flushPending = false
        }
    }

    companion object {

        /** The initial capacity for the candidate queue. */
        protected const val INITIAL_CAPACITY: Int = 10

        /** Get a new instance of an [Overlapper] for the given [strategy] and [trieDepth]. */
        fun of(strategy: Overlap, trieDepth: Int): Overlapper = when (strategy) {
            Ignored -> IgnoredOverlapper()
            Leftmost -> LeftmostOverlapper(trieDepth)
            Longest -> LongestOverlapper(trieDepth)
        }
    }
}
