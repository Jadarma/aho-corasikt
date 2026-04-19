// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.overlap

import io.github.jadarma.ahocorasikt.Overlap

/**
 * Implements [Overlap.Leftmost] strategy.
 *
 * @param buffer The maximum length of a candidate range.
 */
internal class LeftmostOverlapper(private val buffer: Int) : Overlapper() {

    private val candidates = ArrayDeque<IntRange>(INITIAL_CAPACITY)

    override fun offer(range: IntRange) {
        val last = candidates.lastOrNull()

        // If no other candidates, just add it.
        if (last == null) {
            candidates.addLast(range)
            return
        }

        // Because of sorted insertion order, if the range starts before the oldest match, it will overlap all the
        // candidates, so treat it as a special case and clear it.
        if (candidates.first().first >= range.first) {
            candidates.clear()
            candidates.addLast(range)
            return
        }

        // Promote any old ranges that are too far to be overlapped anymore.
        checkInternal(range.last)

        // Going from most recent to old candidates, see where this range should be inserted.
        // Ensure no candidates overlap this range, if any is found with a lower start index, this range is discarded.
        // Along the way, count how many ranges were overlapped and eliminated.
        var toRemove = 0

        for(index in candidates.indices.reversed()) {
            val candidate = candidates[index]
            if (candidate.last < range.first) break
            if (candidate.first < range.first) return
            toRemove++
        }

        repeat(toRemove) { candidates.removeLast() }
        candidates.addLast(range)
    }

    override fun checkInternal(index: Int) {
        val cutoff = index - buffer

        // If far enough, promote everything.
        val lastEnd = candidates.lastOrNull()?.last ?: return
        if (lastEnd < cutoff) {
            promoteAll(candidates)
            candidates.clear()
            return
        }

        // Otherwise, promote as much as possible.
        while (candidates.isNotEmpty()) {
            val candidate = candidates.first().last
            if (candidate <= cutoff) promote(candidates.removeFirst())
            else break
        }
    }

    // Candidates are stored without overlaps, so all remaining candidates are valid.
    override fun finalizeInternal() = promoteAll(candidates)
}
