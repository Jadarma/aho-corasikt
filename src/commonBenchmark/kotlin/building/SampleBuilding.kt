package io.github.jadarma.ahocorasikt.benchmark.building

import com.goncalossilva.resources.Resource
import io.github.jadarma.ahocorasikt.buildTrie
import kotlinx.benchmark.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class SampleBuilding {

    private var words: List<String>? = null

    @Setup
    fun setup() {
        words = Resource("unicode/words.txt")
            .readText()
            .lineSequence()
            .filter(String::isNotEmpty)
            .toList()
    }

    @TearDown
    fun tearDown() {
        words = null
    }

    @Benchmark
    fun build(): Any = buildTrie { addAll(words!!) }
}
