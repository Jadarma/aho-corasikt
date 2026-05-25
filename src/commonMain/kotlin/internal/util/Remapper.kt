// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.util

/**
 * A remapper is used to convert the actual input value into the dense representation.
 * For example, instead of `'a' = 97, 'c' = 99`, it would be `'a'=0, 'c'=1`.
 * This helps keep some trie node representations denser.
 */
internal sealed interface Remapper {

    /**
     * Remaps the [input] to its corresponding internal value.
     * Assumes the value is already positive, since the character values were converted internally.
     * A remapper might return the magic value [Int.MAX_VALUE] instead, if no internal representation exists.
     */
    operator fun get(input: Int): Int

    /**
     * Same as [get], but errors in case of misconfiguration, particularly useful when constructing tries.
     * @throws NoSuchElementException if the remapper does not hold a value for [input].
     */
    fun getValue(input: Int): Int
}

/**
 * A remapper that doesn't do anything.
 * Used when remapping is not needed or not worth the overhead.
 */
internal object NoopRemapper : Remapper {
    override operator fun get(input: Int) = input
    override fun getValue(input: Int) = input
}

/** A remapper that uses a hashmap to remap an alphabet of arbitrary values into a contiguous integer sequence. */
@JvmInline
internal value class MapRemapper private constructor(private val map: Map<Int, Int>) : Remapper {

    constructor(alphabet: Set<Int>) : this(
        alphabet
            .sorted()
            .withIndex()
            .associate { it.value to it.index }
    )

    override operator fun get(input: Int): Int = map[input] ?: Int.MAX_VALUE
    override fun getValue(input: Int): Int = map.getValue(input)
}
