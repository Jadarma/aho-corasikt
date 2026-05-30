package io.github.jadarma.ahocorasikt.benchmark.search

import com.goncalossilva.resources.Resource
import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase.TrieVariant.*
import kotlinx.benchmark.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class LoremIpsumSearch {

    private var text: String? = null

    private var unoptimizedTrie: Trie? = null
    private var compactTrie: Trie? = null
    private var optimizedTrie: Trie? = null
    private var payloadTrie: PayloadTrie<Int>? = null

    @Setup
    fun setup() {
        text = Resource("lorem/sample.txt").readText()
        val words = Resource("lorem/words.txt").readText().lineSequence().filter(String::isNotEmpty)

        val unoptimizedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Unoptimized }
        val compactBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Compact }
        val optimizedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).forceVariant = Optimized }
        val payloadBuilder = PayloadTrie.builder<Int>()

        for (word in words) {
            unoptimizedBuilder.add(word)
            compactBuilder.add(word)
            optimizedBuilder.add(word)
            payloadBuilder.add(word, word.length)
        }

        unoptimizedTrie = unoptimizedBuilder.build()
        compactTrie = compactBuilder.build()
        optimizedTrie = optimizedBuilder.build()
        payloadTrie = payloadBuilder.build()
    }

    @TearDown
    fun tearDown() {
        text = null
        unoptimizedTrie = null
        compactTrie = null
        optimizedTrie = null
        payloadTrie = null
    }

    @Benchmark
    fun run0unoptimizedSearch() = unoptimizedTrie!!.search(text!!).count()

    @Benchmark
    fun run1compactSearch() = compactTrie!!.search(text!!).count()

    @Benchmark
    fun run2optimizedSearch() = optimizedTrie!!.search(text!!).count()

    @Benchmark
    fun run3payloadSearch() = payloadTrie!!.searchWithPayload(text!!).count()
}
