package io.github.jadarma.ahocorasikt.benchmark.search

import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import kotlinx.benchmark.*
import kotlin.random.Random

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class HexadecimalSearch {

    // Constant seed for RNG, to ensure that the same data is used on all compilation targets.
    private val seed = 123456789

    @Param("1000", "10000", "100000")
    var patterns = 0

    private var text: String? = null

    private var defaultTrie: Trie? = null
    private var compactTrie: Trie? = null
    private var remappedTrie: Trie? = null
    private var payloadTrie: PayloadTrie<Char>? = null

    @Setup
    fun setup() {
        val hexadecimal = "0123456789abcdef"
        val rng = Random(seed)
        text = buildString(capacity = 1_000_000) { repeat(1_000_000) { append(hexadecimal.random(rng)) } }

        val defaultBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 0 }
        val remappedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 1 }
        val compactBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 2 }
        val payloadBuilder = PayloadTrie.builder<Char>()

        repeat(patterns) {
            val hash = buildString(capacity = 64) { repeat(64) { append(hexadecimal.random(rng)) } }
            defaultBuilder.add(hash)
            compactBuilder.add(hash)
            remappedBuilder.add(hash)
            payloadBuilder.add(hash, hash.last())
        }

        defaultTrie = defaultBuilder.build()
        compactTrie = compactBuilder.build()
        remappedTrie = remappedBuilder.build()
        payloadTrie = payloadBuilder.build()
    }

    @Benchmark
    fun run0defaultSearch(): Int = defaultTrie!!.search(text!!).count()

    @Benchmark
    fun run1remappedSearch(): Int = remappedTrie!!.search(text!!).count()

    @Benchmark
    fun run2compactSearch(): Int = compactTrie!!.search(text!!).count()

    @Benchmark
    fun run3payloadSearch(): Int = payloadTrie!!.searchWithPayload(text!!).count()
}
