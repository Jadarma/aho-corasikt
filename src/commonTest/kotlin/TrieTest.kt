package io.github.jadarma.ahocorasikt

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TrieTest : FunSpec({

    test("Can build from valid words") {
        val trie = shouldNotThrowAny { Trie.builder().add("hello").add("world").build() }
        trie.search("hello world").count() shouldBe 2
        trie.depth shouldBe 5
        trie.size shouldBe 11
    }

    test("Builder errors on invalid words but is still usable") {
        val builder = Trie.builder()
        shouldThrow<IllegalArgumentException> { builder.add("") }
        val trie = builder.add("hello").add("world").build()
        trie.search("hello world").count() shouldBe 2
        trie.depth shouldBe 5
        trie.size shouldBe 11
    }

    test("Builder can add multiple words at once") {
        val words = setOf("hello", "world")
        val trie = shouldNotThrowAny { Trie.builder().addAll(words).build() }
        trie.search("hello world").count() shouldBe 2
        trie.depth shouldBe 5
        trie.size shouldBe 11
    }

    test("Builder errors on invalid words in bulk and stops there, but is still usable") {
        val words = setOf("hello", "", "world")
        val builder = Trie.builder()
        shouldThrow<IllegalArgumentException> { builder.addAll(words) }
        val trie = builder.build()
        trie.search("hello world").count() shouldBe 1 // Hits hello, but not world.
        trie.depth shouldBe 5
        trie.size shouldBe 6
    }

    test("Builder errors on being built twice") {
        val builder = Trie.builder().add("hello")
        shouldNotThrowAny { builder.build() }
        shouldThrow<IllegalStateException> { builder.build() }
        shouldThrow<IllegalStateException> { builder.add("world") }
        shouldThrow<IllegalStateException> { builder.addAll(listOf("there")) }
    }

    test("Building an empty trie is an error") {
        shouldThrow<IllegalArgumentException> { Trie.builder().build() }
    }

    test("Uses correct match settings defaults") {
        val trie = Trie.builder().add("hello").add("world").build()
        trie.caseSensitivity shouldBe CaseSensitivity.Sensitive
        trie.matchType shouldBe MatchType.Unrestricted
        trie.overlap shouldBe Overlap.Allowed
    }

    test("Respects user overrides for match settings") {
        val trie = Trie.builder(
            caseSensitivity = CaseSensitivity.AsciiInsensitive,
            matchType = MatchType.WholeWords,
            overlap = Overlap.Leftmost,
        ).add("hello").add("world").build()
        trie.caseSensitivity shouldBe CaseSensitivity.AsciiInsensitive
        trie.matchType shouldBe MatchType.WholeWords
        trie.overlap shouldBe Overlap.Leftmost
    }
})
