package io.github.jadarma.ahocorasikt.internal.util

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RemapperTest : FunSpec({

    test("NoopRemapper is useless") {
        val mapper: Remapper = NoopRemapper
        mapper[1] shouldBe 1
        mapper.getValue(1) shouldBe 1
    }

    context("MapRemapper") {
        val alphabet = setOf(20, 40 ,80, 90, 60, 10)
        val mapper = MapRemapper(alphabet)

        test("maps items in order") {
            mapper[10] shouldBe 0
            mapper[20] shouldBe 1
            mapper[40] shouldBe 2
            mapper[60] shouldBe 3
            mapper[80] shouldBe 4
            mapper[90] shouldBe 5
            mapper[99] shouldBe Int.MAX_VALUE
        }

        test("gets values in order") {
            mapper.getValue(10) shouldBe 0
            mapper.getValue(20) shouldBe 1
            mapper.getValue(40) shouldBe 2
            mapper.getValue(60) shouldBe 3
            mapper.getValue(80) shouldBe 4
            mapper.getValue(90) shouldBe 5
            shouldThrowExactly<NoSuchElementException> { mapper.getValue(99) }
        }
    }
})
