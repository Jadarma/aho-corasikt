package io.github.jadarma.ahocorasikt.benchmark.search

import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import kotlinx.benchmark.*
import kotlin.random.Random

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class RandomAsciiSearch {

    // Constant seed for RNG, to ensure that the same data is used on all compilation targets.
    private val seed = 123456789

    @Param("100", "1000", "10000", "100000")
    var patterns = 0

    @Param("100", "10000", "100000", "1000000")
    var textLength = 0

    private var text: String? = null

    private var defaultTrie: Trie? = null
    private var compactTrie: Trie? = null
    private var remappedTrie: Trie? = null

    @Setup
    fun setup() {
        val ascii = buildString { for (i in 0..126) append(i.toChar()) }
        val rng = Random(seed)
        val sb = StringBuilder(textLength)
        val defaultBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 0 }
        val compactBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 1 }
        val remappedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 2 }

        repeat(patterns) {
            repeat(times = rng.nextInt(2, 15)) {
                sb.append(ascii.random(rng))
            }
            val keyword = sb.toString()
            defaultBuilder.add(keyword)
            compactBuilder.add(keyword)
            remappedBuilder.add(keyword)
            sb.clear()
        }

        defaultTrie = defaultBuilder.build()
        compactTrie = compactBuilder.build()
        remappedTrie = remappedBuilder.build()

        repeat(textLength) {
            sb.append(ascii.random(rng))
        }
        text = sb.toString()
    }

    @TearDown
    fun tearDown() {
        text = null
        defaultTrie = null
        compactTrie = null
        remappedTrie = null
    }

    @Benchmark
    fun run0defaultSearch(): Int = defaultTrie!!.search(text!!).count()

    @Benchmark
    fun run1remappedSearch(): Int = remappedTrie!!.search(text!!).count()

    @Benchmark
    fun run2compactSearch(): Int = compactTrie!!.search(text!!).count()
}
