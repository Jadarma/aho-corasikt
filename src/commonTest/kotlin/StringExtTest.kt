package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.test.ofVariants
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.sequences.shouldBeEmpty
import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.shouldBe

class StringExtTest : FunSpec({

    val text = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit,
        sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
    """.trimIndent().replace("\n", " ")

    val unmatching = "🚫"

    val words = setOf("ore", "et", "ipsum", "amet", "magna")
    val trie = Trie.ofVariants(words)

    context("String matching") {
        test("using matches function") {
            (text matches trie).shouldBeTrue()
            (unmatching matches trie).shouldBeFalse()
        }

        test("using in notation") {
            (trie in text).shouldBeTrue()
            (trie in unmatching).shouldBeFalse()
        }
    }

    test("Counting") {
        text.count(trie) shouldBe 9
        unmatching.count(trie) shouldBe 0
    }

    context("Finding keywords in strings") {
        test("can find all ranges") {
            unmatching.findRanges(trie).shouldBeEmpty()
            text.findRanges(trie).shouldContainExactly(
                1..3,
                6..10,
                22..25,
                24..25,
                35..36,
                96..98,
                100..101,
                106..108,
                110..114,
            )
        }
        test("can find all matches") {
            unmatching.find(trie).shouldBeEmpty()
            text.find(trie).shouldContainExactly(
                1 to "ore",
                6 to "ipsum",
                22 to "amet",
                24 to "et",
                35 to "et",
                96 to "ore",
                100 to "et",
                106 to "ore",
                110 to "magna",
            )
        }

        test("can find the first match") {
            unmatching.findFirst(trie).shouldBeNull()
            text.findFirst(trie).shouldNotBeNull().shouldBe("ore")
        }

        test("can find the last match") {
            unmatching.findLast(trie).shouldBeNull()
            text.findLast(trie).shouldNotBeNull().shouldBe("magna")
        }

        test("can find the first with index") {
            shouldThrow<IndexOutOfBoundsException> { text.findAnyOf(trie, Int.MAX_VALUE) }
            shouldThrow<IndexOutOfBoundsException> { text.findAnyOf(trie, -1) }
            unmatching.findAnyOf(trie).shouldBeNull()
            text.findAnyOf(trie) shouldBe (1 to "ore")
            text.findAnyOf(trie, startIndex = 10) shouldBe (22 to "amet")
            text.findAnyOf(trie, startIndex = 111).shouldBeNull()
        }

        test("can find the last with index") {
            shouldThrow<IndexOutOfBoundsException> { text.findLastAnyOf(trie, Int.MAX_VALUE) }
            shouldThrow<IndexOutOfBoundsException> { text.findLastAnyOf(trie, -1) }
            unmatching.findLastAnyOf(trie).shouldBeNull()
            text.findLastAnyOf(trie) shouldBe (110 to "magna")
            text.findLastAnyOf(trie, startIndex = 10) shouldBe (110 to "magna")
            text.findLastAnyOf(trie, startIndex = 111).shouldBeNull()
        }

        test("can find the index of first match") {
            unmatching.indexOfFirst(trie) shouldBe -1
            text.indexOfFirst(trie) shouldBe 1
        }

        test("can find the index of last match") {
            unmatching.indexOfLast(trie) shouldBe -1
            text.indexOfLast(trie) shouldBe 110
        }
    }

    context("Splitting text by matches") {
        test("validates the limit") {
            shouldThrow<IllegalArgumentException> { text.split(trie, limit = -1) }
        }

        test("tokenizes the text") {
            unmatching.split(trie).shouldBeSingleton { it shouldBe unmatching }
            text.split(trie).shouldContainExactly(
                "L", "ore", "m ", "ipsum", " dolor sit ", "amet", ", consect", "et",
                "ur adipiscing elit, sed do eiusmod tempor incididunt ut lab", "ore", " ", "et",
                " dol", "ore", " ", "magna", " aliqua.",
            )
        }

        test("respects the limit") {
            unmatching.split(trie, limit = 3).shouldBeSingleton { it shouldBe unmatching }
            text.split(trie, limit = 3).shouldContainExactly(
                "L", "ore", "m ", "ipsum", " dolor sit ", "amet",
                text.substringAfter("amet"),
            )
        }

        test("consecutive matches still produce the empty in-between") {
            "ipsumamet".split(trie).shouldContainExactly("", "ipsum", "", "amet")
        }
    }

    context("Mapping over text") {
        test("does nothing on no matches") {
            unmatching
                .map(trie) { fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .mapNotNull(trie) { fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .mapIndexed(trie) { _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .mapIndexedNotNull(trie) { _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .flatMap<Unit>(trie) { fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .flatMapIndexed<Unit>(trie) { _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
        }

        test("can map simple values") {
            text
                .map(trie) { it.length }
                .shouldContainExactly(3, 5, 4, 2, 2, 3, 2, 3, 5)
        }

        test("can filter nulls in simple values") {
            text
                .mapNotNull(trie) { match -> match.length.takeIf { it > 2 } }
                .shouldContainExactly(3, 5, 4, 3, 3, 5)
        }

        test("can map values with index") {
            text
                .mapIndexed(trie) { index, match -> index to match.length }
                .shouldContainExactly(
                    1 to 3,
                    6 to 5,
                    22 to 4,
                    24 to 2,
                    35 to 2,
                    96 to 3,
                    100 to 2,
                    106 to 3,
                    110 to 5,
                )
        }

        test("can filter nulls in values with index") {
            text
                .mapIndexedNotNull(trie) { index, match -> (index to match.length).takeIf { match.length > 2 } }
                .shouldContainExactly(
                    1 to 3,
                    6 to 5,
                    22 to 4,
                    96 to 3,
                    106 to 3,
                    110 to 5,
                )
        }

        test("can flatmap simple values") {
            text
                .flatMap(trie) { listOf(it.length, it.first()) }
                .shouldContainExactly(3, 'o', 5, 'i', 4, 'a', 2, 'e', 2, 'e', 3, 'o', 2, 'e', 3, 'o', 5, 'm')
        }
        test("can flatmap values with index") {
            text
                .flatMapIndexed(trie) { index, match -> listOf(index, match.first()) }
                .shouldContainExactly(1, 'o', 6, 'i', 22, 'a', 24, 'e', 35, 'e', 96, 'o', 100, 'e', 106, 'o', 110, 'm')
        }
    }

    context("Modifying text") {
        test("makes no modifications if no matches") {
            unmatching.remove(trie) shouldBe unmatching
            unmatching.replace(trie, "#") shouldBe unmatching
            unmatching.replace(trie) { fail("should not be called") } shouldBe unmatching
        }

        test("can remove occurrences") {
            text.remove(trie) shouldBe """
                Lm  dolor sit , consectur adipiscing elit,
                sed do eiusmod tempor incididunt ut lab  dol  aliqua.
            """.trimIndent().replace("\n", " ")
        }

        test("can replace with constant") {
            text.replace(trie, "***") shouldBe """
                L***m *** dolor sit ***, consect***ur adipiscing elit,
                sed do eiusmod tempor incididunt ut lab*** *** dol*** *** aliqua.
            """.trimIndent().replace("\n", " ")
        }

        test("can replace with function") {
            text.replace(trie) { "*".repeat(it.length) } shouldBe """
                L***m ***** dolor sit ****, consect**ur adipiscing elit,
                sed do eiusmod tempor incididunt ut lab*** ** dol*** ***** aliqua.
            """.trimIndent().replace("\n", " ")
        }

        test("can reformat entire input") {
            text
                .reformat(trie) { isMatch, span -> if (isMatch) "<$span>" else span.uppercase() }
                .shouldBe(
                    """
                        L<ore>M <ipsum> DOLOR SIT <amet>, CONSECT<et>UR ADIPISCING ELIT,
                        SED DO EIUSMOD TEMPOR INCIDIDUNT UT LAB<ore> <et> DOL<ore> <magna> ALIQUA.
                    """.trimIndent().replace("\n", " ")
                )
        }
    }
})
