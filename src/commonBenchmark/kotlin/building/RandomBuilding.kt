package io.github.jadarma.ahocorasikt.benchmark.building

import io.github.jadarma.ahocorasikt.Trie
import kotlinx.benchmark.*
import kotlin.random.Random

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class RandomBuilding {

    @Param("10", "100", "1000", "10000", "100000")
    var patterns = 0

    private val ascii = ArrayList<CharSequence>()
    private val unicode = ArrayList<CharSequence>()
    private val hashes = ArrayList<CharSequence>()

    @Setup
    fun prepare() {
        val sb = StringBuilder(256)

        // ASCII
        val asciiAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        repeat(patterns) {
            repeat(times = Random.nextInt(2, 15)) {
                sb.append(asciiAlphabet.random())
            }
            ascii.add(sb.toString())
            sb.clear()
        }

        // Random Unicode
        repeat(patterns) {
            repeat(times = Random.nextInt(2, 15)) {
                sb.append(Random.nextInt(until = Char.MAX_VALUE.code).toChar())
            }
            unicode.add(sb.toString())
            sb.clear()
        }

        // Hashes
        val hashesAlphabet = "0123456789abcdef"
        repeat(patterns) {
            repeat(times = 64) {
                sb.append(hashesAlphabet.random())
            }
            hashes.add(sb.toString())
            sb.clear()
        }
    }

    @TearDown
    fun cleanup() {
        ascii.clear()
        unicode.clear()
        hashes.clear()
    }

    @Benchmark
    fun asciiWords(): Trie {
        return Trie.builder().addAll(ascii).build()
    }

    @Benchmark
    fun unicodeWords(): Trie {
        return Trie.builder().addAll(unicode).build()
    }

    @Benchmark
    fun hashLike(): Trie {
        return Trie.builder().addAll(hashes).build()
    }
}
