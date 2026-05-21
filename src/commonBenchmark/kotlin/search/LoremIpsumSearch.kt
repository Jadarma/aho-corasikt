package io.github.jadarma.ahocorasikt.benchmark.search

import com.goncalossilva.resources.Resource
import io.github.jadarma.ahocorasikt.PayloadTrie
import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilderBase
import kotlinx.benchmark.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class LoremIpsumSearch {

    private var text: String? = null

    private var defaultTrie: Trie? = null
    private var compactTrie: Trie? = null
    private var remappedTrie: Trie? = null
    private var payloadTrie: PayloadTrie<Int>? = null

    @Setup
    fun setup() {
        text = Resource("lorem/sample.txt").readText()
        val words = Resource("lorem/words.txt").readText().lineSequence().filter(String::isNotEmpty)

        val defaultBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 0 }
        val remappedBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 1 }
        val compactBuilder = Trie.builder().apply { (this as TrieBuilderBase<*>).optimizationLevel = 2 }
        val payloadBuilder = PayloadTrie.builder<Int>()

        for (word in words) {
            defaultBuilder.add(word)
            compactBuilder.add(word)
            remappedBuilder.add(word)
            payloadBuilder.add(word, word.length)
        }

        defaultTrie = defaultBuilder.build()
        compactTrie = compactBuilder.build()
        remappedTrie = remappedBuilder.build()
        payloadTrie = payloadBuilder.build()
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

    @Benchmark
    fun run3payloadSearch(): Int = payloadTrie!!.searchWithPayload(text!!).count()
}
