package io.github.jadarma.ahocorasikt.benchmark.search

import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase.TrieVariant.*
import kotlinx.benchmark.*
import kotlin.random.Random

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class RandomAsciiSearch {

    // Constant seed for RNG, to ensure that the same data is used on all compilation targets.
    private val seed = 123456789

    @Param("100", "10000", "100000")
    var patterns = 0

    @Param("100", "10000", "1000000")
    var textLength = 0

    private var text: String? = null

    private var unoptimizedTrie: Trie? = null
    private var compactTrie: Trie? = null
    private var optimizedTrie: Trie? = null

    @Setup
    fun setup() {
        val ascii = buildString { for (i in 0..126) append(i.toChar()) }
        val rng = Random(seed)
        val sb = StringBuilder(textLength)

        val unoptimizedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Unoptimized }
        val compactBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Compact }
        val optimizedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Optimized }

        repeat(patterns) {
            repeat(times = rng.nextInt(2, 15)) {
                sb.append(ascii.random(rng))
            }
            val keyword = sb.toString()
            unoptimizedBuilder.add(keyword)
            compactBuilder.add(keyword)
            optimizedBuilder.add(keyword)
            sb.clear()
        }

        unoptimizedTrie = unoptimizedBuilder.build()
        compactTrie = compactBuilder.build()
        optimizedTrie = optimizedBuilder.build()

        repeat(textLength) {
            sb.append(ascii.random(rng))
        }
        text = sb.toString()
    }

    @TearDown
    fun tearDown() {
        text = null
        unoptimizedTrie = null
        compactTrie = null
        optimizedTrie = null
    }

    @Benchmark
    fun run0unoptimizedSearch() = unoptimizedTrie!!.search(text!!).count()

    @Benchmark
    fun run1compactSearch() = compactTrie!!.search(text!!).count()

    @Benchmark
    fun run2optimizedSearch() = optimizedTrie!!.search(text!!).count()
}
