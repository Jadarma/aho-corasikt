// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.overlap

import io.github.jadarma.ahocorasikt.Overlap
import kotlin.jvm.JvmStatic

/**
 * Implements [Overlap.Longest] strategy.
 *
 * @param buffer The maximum length of a candidate range.
 */
internal class LongestOverlapper(private val buffer: Int) : Overlapper() {

    private val candidates = ArrayDeque<IntRange>(INITIAL_CAPACITY)

    override fun offer(range: IntRange) {
        checkAndPromote(range.last)

        // If the range overlaps the last candidate and is not larger, we can eliminate it.
        val candidate = candidates.lastOrNull()
        if(candidate != null && candidate.last >= range.first && range.size <= candidate.size) return

        candidates.addLast(range)
    }

    override fun checkInternal(index: Int) {
        // If the index is far enough, all candidates can be checked.
        if (checkAndPromote(index)) return

        // Try to find a sub-problem by searching for rightmost node that does not overlap to the right and is far
        // enough for any future overlaps.
        if (candidates.size < 2) return
        var minStart = index - buffer
        var index = candidates.lastIndex - 1

        while (index >= 0) {
            val range = candidates[index]
            minStart = minOf(minStart, candidates[index + 1].first)
            if (range.last < minStart) break
            index--
        }

        // Otherwise, the overlaps are still non-deterministic, so we need to wait until later.
        if (index == -1) return

        filterCandidates(candidates.subList(0, index + 1))
    }

    override fun finalizeInternal() = filterCandidates(candidates)

    /**
     * Checks if the [index] is far away from the [candidates] to process them.
     * If so, overlaps are removed and remaining candidates are promoted.
     * Returns whether the promotion succeeded.
     */
    private fun checkAndPromote(index: Int): Boolean {
        val lastEnd = candidates.lastOrNull()?.last ?: return true
        if (lastEnd >= index - buffer) return false
        filterCandidates(candidates)
        return true
    }

    private fun filterCandidates(candidates: MutableList<IntRange>) {
        // Sort the candidates by their overlap priority.
        candidates.sortWith(comparator = byPriority)

        // Sieve out the overlaps.
        var domIndex = 0
        while (domIndex < candidates.lastIndex) {
            val dominant = candidates[domIndex]
            var subIndex = domIndex + 1
            while (subIndex <= candidates.lastIndex) {
                if (dominant overlaps candidates[subIndex]) {
                    candidates.removeAt(subIndex)
                } else {
                    subIndex++
                }
            }
            domIndex++
        }

        // Reorder the remaining non-overlapping candidates to their insertion order, and promote them.
        candidates.sortWith(comparator = byEmitOrder)
        promoteAll(candidates)
        candidates.clear()
    }

    private companion object {

        val byPriority: Comparator<IntRange> = compareByDescending<IntRange> { it.size }.thenBy(IntRange::first)

        val byEmitOrder: Comparator<IntRange> = compareBy(IntRange::last, IntRange::first)

        /** Returns the size of the range. */
        @JvmStatic
        val IntRange.size: Int get() = last - first + 1

        /** Returns `true` if these two ranges overlap on at least one index. */
        @JvmStatic
        infix fun IntRange.overlaps(other: IntRange): Boolean = this.first <= other.last && this.last >= other.first
    }
}
