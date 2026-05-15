package io.github.jadarma.ahocorasikt

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PayloadTrieTest : FunSpec({
    test("Can build from valid words") {
        val trie = shouldNotThrowAny {
            PayloadTrie.builder<Int>()
                .add("hello", 1)
                .add("world", 2)
                .build()
        }
        trie.searchWithPayload("hello world").sumOf { it.second } shouldBe 3
        trie.depth shouldBe 5
        trie.size shouldBe 11
    }

    test("Builder errors on invalid words but is still usable") {
        val builder = PayloadTrie.builder<Int>()
        shouldThrow<IllegalArgumentException> { builder.add("", 0) }
        val trie = builder.add("hello", 1).add("world", 2).build()
        trie.searchWithPayload("hello world").sumOf { it.second } shouldBe 3
        trie.depth shouldBe 5
        trie.size shouldBe 11
    }

    test("Builder can add multiple words at once") {
        val words = setOf("hello", "world")
        val trie = shouldNotThrowAny { PayloadTrie.builder<Int>().addAll(words, 2).build() }
        trie.searchWithPayload("hello world").sumOf { it.second } shouldBe 4
        trie.depth shouldBe 5
        trie.size shouldBe 11
    }

    test("Builder errors on invalid words in bulk and stops there, but is still usable") {
        val words = setOf("hello", "", "world")
        val builder = PayloadTrie.builder<Int>()
        shouldThrow<IllegalArgumentException> { builder.addAll(words, 1) }
        val trie = builder.build()
        trie.searchWithPayload("hello world").sumOf { it.second } shouldBe 1 // Hits hello, but not world.
        trie.depth shouldBe 5
        trie.size shouldBe 6
    }

    test("Builder errors on being built twice") {
        val builder = PayloadTrie.builder<Int>().add("hello", 0)
        shouldNotThrowAny { builder.build() }
        shouldThrow<IllegalStateException> { builder.build() }
        shouldThrow<IllegalStateException> { builder.add("world", 1) }
        shouldThrow<IllegalStateException> { builder.addAll(listOf("everyone"), 1) }
        shouldThrow<IllegalStateException> { builder.addAll(mapOf("there" to 3)) }
    }

    test("Building an empty trie is an error") {
        shouldThrow<IllegalArgumentException> { PayloadTrie.builder<Int>().build() }
    }

    test("Uses correct match settings defaults") {
        val trie = PayloadTrie.builder<Int>().add("hello", 0).build()
        trie.caseSensitivity shouldBe CaseSensitivity.Sensitive
        trie.matchType shouldBe MatchType.Unrestricted
        trie.overlap shouldBe Overlap.Allowed
    }

    test("Respects user overrides for match settings") {
        val trie = PayloadTrie.builder<Int>(
            caseSensitivity = CaseSensitivity.AsciiInsensitive,
            matchType = MatchType.WholeWords,
            overlap = Overlap.Leftmost,
        ).add("hello", 0).build()
        trie.caseSensitivity shouldBe CaseSensitivity.AsciiInsensitive
        trie.matchType shouldBe MatchType.WholeWords
        trie.overlap shouldBe Overlap.Leftmost
    }
})
