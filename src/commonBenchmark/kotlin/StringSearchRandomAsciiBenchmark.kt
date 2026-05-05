package io.github.jadarma.ahocorasikt.benchmark

import io.github.jadarma.ahocorasikt.Trie
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilder
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlin.random.Random

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class StringSearchRandomAsciiBenchmark {

    // Constant seed for RNG, to ensure that the same data is used on all compilation targets.
    private val seed = 1337_420_69_67

    @Param("10", "100", "1000", "10000", "100000")
    var patterns = 0

    @Param("10", "100", "1000", "10000", "100000", "1000000")
    var textLength = 0

    private var text: String? = null

    private var fastTrie: Trie? = null

    private var slowTrie: Trie? = null

    @Setup
    fun setup() {
        val ascii = buildString { for(i in 0..126) append(i.toChar()) }
        val rng = Random(seed)
        val sb = StringBuilder(textLength)
        val fastBuilder = Trie.builder()
        val slowBuilder = Trie.builder()

        repeat(patterns) {
            repeat(times = rng.nextInt(2, 15)) {
                sb.append(ascii.random(rng))
            }
            val keyword = sb.toString()
            fastBuilder.add(keyword)
            slowBuilder.add(keyword)
            sb.clear()
        }
        fastTrie = fastBuilder.build()
        slowTrie = (slowBuilder as TrieBuilder).apply { disableOptimization = true }.build()

        repeat(textLength) {
            sb.append(ascii.random(rng))
        }
        text = sb.toString()
    }

    @TearDown
    fun tearDown() {
        fastTrie = null
        slowTrie = null
        text = null
    }

    @Benchmark
    fun optimizedSearch(): Int {
        val trie = checkNotNull(fastTrie)
        val text = checkNotNull(text)
        return trie.search(text).count()
    }

    @Benchmark
    fun unoptimizedSearch(): Int {
        val trie = checkNotNull(slowTrie)
        val text = checkNotNull(text)
        return trie.search(text).count()
    }
}
