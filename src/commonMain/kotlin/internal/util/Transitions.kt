// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.util

import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderNode
import io.github.jadarma.ahocorasikt.internal.nodes.OptimizedTrieNodes

/**
 * A transition builder for [OptimizedTrieNodes].
 * Builds the double array trie _(technically, the `next` and `check` components of a triple array trie).
 * The internal array expands dynamically.
 *
 * Call the [compact] function to receive a finalized array fit for production use.
 * Since both values are always used together, they are stored as halves of a `long` value, the `check` being stored in
 * the least significant bits.
 *
 * @param remap The remapper the trie would use to calculate offsets.
 * @param capacity How many candidate slots to keep track of.
 * @param attempts How many times can a slot fail insertion before being eliminated as a candidate.
 */
internal class Transitions(
    private val remap: Remapper,
    private val capacity: Int = 256,
    private val attempts: Int = 16,
) {

    /** A dynamically growing list of segments that will eventually become the transition array. */
    private val buckets: ArrayList<LongArray> = ArrayList()

    /** The size of the transition array. */
    private var size: Int = 0

    /**
     * The current minimum insertion offset.
     * All values lower than this have failed too many times and are considered unusable.
     */
    private var offset = 0

    /**
     * A window of queued candidates.
     * Each index tracks the number of failures counted for each slot relative to the base [offset].
     */
    private val window = ArrayDeque<Int>(initialCapacity = capacity).apply {
        repeat(capacity) { addLast(0) }
    }

    /**
     * Gets the transition record at the given [index].
     * The lookup is safe even if out of bounds.
     */
    operator fun get(index: Int): Long {
        val bucket = index ushr BUCKET_SIZE_BITS
        return if (bucket < buckets.size) buckets[bucket][index and BUCKET_MASK] else -1
    }

    /**
     * Sets the transition record at the given [index] to the [value].
     * Auto-expands the internal array to fit.
     */
    operator fun set(index: Int, value: Long) {
        val lastBucket = buckets.size - 1
        val bucket = index ushr BUCKET_SIZE_BITS
        if (bucket > lastBucket) {
            buckets.ensureCapacity(bucket)
            for (i in lastBucket.inc()..bucket) {
                buckets.add(i, LongArray(BUCKET_SIZE) { -1L })
            }
        }
        buckets[bucket][index and BUCKET_MASK] = value
        size = maxOf(size, index + 1)
    }

    /**
     * Find a suitable place to insert the transitions of the [node], and return it's `base` offset value.
     * @return The value of the `base` of this [node].
     * @throws IllegalStateException if there is no more room to insert the [node].
     */
    fun insert(node: TrieBuilderNode): Int {
        if (node.children.isEmpty()) return Int.MAX_VALUE

        // Update candidates, only keep valid ones.
        while (window.isNotEmpty() && window.first() >= attempts) {
            window.removeFirst()
            if (Int.MAX_VALUE - capacity < offset++) continue
            window.addLast(0)
        }
        check(window.isNotEmpty()) { BUILD_ERROR_MESSAGE }

        // Attempt to pass through the candidates.
        for (candidate in 0..<window.size) {
            val slot = offset + candidate
            if (attempt(node, slot)) return slot
            window[candidate] += 1
        }

        // All candidates fail, but that does not mean insertion is impossible.
        // For this case, attempt to walk the rest manually, without keeping track.
        var extra = offset + window.size
        while (true) {
            if (attempt(node, extra)) return extra
            check(extra++ < Int.MAX_VALUE) { BUILD_ERROR_MESSAGE }
        }
    }

    /**
     * Attempt to insert the transitions of the [node] at the given [offset].
     * Returns whether insertion was successful.
     * If so, the [offset] should be used as the base value for the [node].
     */
    private fun attempt(node: TrieBuilderNode, offset: Int): Boolean {
        val found = node.children.keys.all { label ->
            val input = remap.getValue(label)
            require(Int.MAX_VALUE - input > offset) { BUILD_ERROR_MESSAGE }
            val slot = offset + input
            this[slot].toInt() == -1
        }

        if (!found) return false

        node.children.forEach { (label, child) ->
            val slot = offset + remap.getValue(label)
            check(this[slot] == -1L)
            this[slot] = child.index.toLong().shl(Int.SIZE_BITS) or node.index.toLong()
        }

        return true
    }

    /** Finalize building transitions and return a compacted value array. */
    fun compact(): LongArray {
        val array = LongArray(size)
        for (bucketIndex in buckets.indices) {
            val bucket = buckets[bucketIndex]
            val last = bucketIndex == buckets.lastIndex
            val copy = if (last) size.rem(BUCKET_SIZE) else BUCKET_SIZE
            bucket.copyInto(array, bucketIndex * BUCKET_SIZE, 0, copy)
        }
        return array
    }

    private companion object {
        const val BUCKET_SIZE_BITS = 10
        const val BUCKET_SIZE = 1 shl BUCKET_SIZE_BITS
        const val BUCKET_MASK = BUCKET_SIZE - 1
        const val BUILD_ERROR_MESSAGE = "Cannot build. Trie too large."
    }
}
