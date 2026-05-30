package io.github.jadarma.ahocorasikt.benchmark.search

import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase.TrieVariant.*
import kotlinx.benchmark.*
import kotlin.random.Random

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class HexadecimalSearch {

    // Constant seed for RNG, to ensure that the same data is used on all compilation targets.
    private val seed = 123456789

    @Param("100", "1000", "10000")
    var patterns = 0

    private var text: String? = null

    private var unoptimizedTrie: Trie? = null
    private var compactTrie: Trie? = null
    private var optimizedTrie: Trie? = null
    private var payloadTrie: PayloadTrie<Char>? = null

    @Setup
    fun setup() {
        val hexadecimal = "0123456789abcdef"
        val rng = Random(seed)
        text = buildString(capacity = 1_000_000) { repeat(1_000_000) { append(hexadecimal.random(rng)) } }

        val unoptimizedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Unoptimized }
        val compactBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Compact }
        val optimizedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Optimized }
        val payloadBuilder = PayloadTrie.builder<Char>()

        repeat(patterns) {
            val hash = buildString(capacity = 64) { repeat(64) { append(hexadecimal.random(rng)) } }
            unoptimizedBuilder.add(hash)
            compactBuilder.add(hash)
            optimizedBuilder.add(hash)
            payloadBuilder.add(hash, hash.last())
        }

        unoptimizedTrie = unoptimizedBuilder.build()
        compactTrie = compactBuilder.build()
        optimizedTrie = optimizedBuilder.build()
        payloadTrie = payloadBuilder.build()
    }

    @Benchmark
    fun run0unoptimizedSearch() = unoptimizedTrie!!.search(text!!).count()

    @Benchmark
    fun run1compactSearch() = compactTrie!!.search(text!!).count()

    @Benchmark
    fun run2optimizedSearch() = optimizedTrie!!.search(text!!).count()

    @Benchmark
    fun run3payloadSearch() = payloadTrie!!.search(text!!).count()
}
