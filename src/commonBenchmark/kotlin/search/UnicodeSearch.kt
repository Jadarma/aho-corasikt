package io.github.jadarma.ahocorasikt.benchmark.search

import com.goncalossilva.resources.Resource
import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.Overlap
import io.github.jadarma.ahocorasikt.Trie
import kotlinx.benchmark.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class UnicodeSearch {

    private var text: String? = null

    private var defaultTrie: Trie? = null
    private var insensitiveTrie: Trie? = null
    private var wholeWordTrie: Trie? = null
    private var noOverlapTrie: Trie? = null
    private var fullOptionTrie: Trie? = null

    @Setup
    fun setup() {
        text = Resource("unicode/sample.txt").readText()
        val words = Resource("unicode/words.txt").readText().lineSequence().filter(String::isNotEmpty)

        val defaultBuilder = Trie.builder()
        val insensitiveBuilder = Trie.builder(caseSensitivity = CaseSensitivity.SafeInsensitive)
        val wholeWordBuilder = Trie.builder(matchType = MatchType.WholeWords)
        val noOverlapBuilder = Trie.builder(overlap = Overlap.Leftmost)
        val fullOptionBuilder = Trie.builder(
            caseSensitivity = CaseSensitivity.SafeInsensitive,
            matchType = MatchType.WholeWords,
            overlap = Overlap.Leftmost
        )

        for (word in words) {
            defaultBuilder.add(word)
            insensitiveBuilder.add(word)
            wholeWordBuilder.add(word)
            noOverlapBuilder.add(word)
            fullOptionBuilder.add(word)
        }

        defaultTrie = defaultBuilder.build()
        insensitiveTrie = insensitiveBuilder.build()
        wholeWordTrie = wholeWordBuilder.build()
        noOverlapTrie = noOverlapBuilder.build()
        fullOptionTrie = fullOptionBuilder.build()
    }

    @TearDown
    fun tearDown() {
        text = null
        defaultTrie = null
        insensitiveTrie = null
        wholeWordTrie = null
        noOverlapTrie = null
        fullOptionTrie = null
    }

    @Benchmark
    fun defaultSearch(): Int = defaultTrie!!.search(text!!).count()

    @Benchmark
    fun insensitiveSearch(): Int = insensitiveTrie!!.search(text!!).count()

    @Benchmark
    fun wholeWordSearch(): Int = wholeWordTrie!!.search(text!!).count()

    @Benchmark
    fun noOverlapSearch(): Int = noOverlapTrie!!.search(text!!).count()

    @Benchmark
    fun fullOptionSearch(): Int = fullOptionTrie!!.search(text!!).count()
}
