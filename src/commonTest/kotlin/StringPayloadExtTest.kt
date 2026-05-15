package io.github.jadarma.ahocorasikt

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

class StringPayloadExtTest : FunSpec({

    val text = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit,
        sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
    """.trimIndent().replace("\n", " ")

    val unmatching = "🚫"

    // In this example, the payload is just the length of the match.
    val words = mapOf<CharSequence, Int>(
        "ore" to 3,
        "et" to 2,
        "ipsum" to 5,
        "amet" to 4,
        "magna" to 5,
    )

    val trie = trieOf(words)

    context("String matching") {
        test("using matches function") {
            (text matches trie).shouldBeTrue()
            (unmatching matches trie).shouldBeFalse()
        }

        test("using in notation") {
            (trie in text).shouldBeTrue()
            (trie in unmatching).shouldBeFalse()
        }

        test("using specific payload") {
            unmatching.contains(trie, 9).shouldBeFalse()
            unmatching.contains(trie, 5).shouldBeFalse()
            text.contains(trie, 9).shouldBeFalse()
            text.contains(trie, 5).shouldBeTrue()
        }
    }


    context("Counting") {
        test("all matches") {
            unmatching.count(trie) shouldBe 0
            text.count(trie) shouldBe 9
        }

        test("using specific payload") {
            unmatching.count(trie, 9) shouldBe 0
            unmatching.count(trie, 5) shouldBe 0
            text.count(trie, 9) shouldBe 0
            text.count(trie, 5) shouldBe 2
        }
    }

    context("Finding payloads in strings") {
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

        test("can find all matches with payload") {
            unmatching.findWithPayload(trie).shouldBeEmpty()
            text.findWithPayload(trie).shouldContainExactly(
                Triple(1, "ore", 3),
                Triple(6, "ipsum", 5),
                Triple(22, "amet", 4),
                Triple(24, "et", 2),
                Triple(35, "et", 2),
                Triple(96, "ore", 3),
                Triple(100, "et", 2),
                Triple(106, "ore", 3),
                Triple(110, "magna", 5),
            )
        }

        test("can find the first match") {
            unmatching.findFirst(trie).shouldBeNull()
            text.findFirst(trie).shouldNotBeNull().shouldBe("ore")
        }

        test("can find the first payload") {
            unmatching.findFirstPayload(trie).shouldBeNull()
            text.findFirstPayload(trie).shouldNotBeNull().shouldBe(3)
        }

        test("can find the last match") {
            unmatching.findLast(trie).shouldBeNull()
            text.findLast(trie).shouldNotBeNull().shouldBe("magna")
        }

        test("can find the last payload") {
            unmatching.findLastPayload(trie).shouldBeNull()
            text.findLastPayload(trie).shouldNotBeNull().shouldBe(5)
        }

        test("can find the first match with index") {
            shouldThrow<IndexOutOfBoundsException> { text.findAnyOf(trie, Int.MAX_VALUE) }
            shouldThrow<IndexOutOfBoundsException> { text.findAnyOf(trie, -1) }
            unmatching.findAnyOf(trie).shouldBeNull()
            text.findAnyOf(trie) shouldBe (1 to "ore")
            text.findAnyOf(trie, startIndex = 10) shouldBe (22 to "amet")
            text.findAnyOf(trie, startIndex = 111).shouldBeNull()
        }

        test("can find the first payload with index") {
            shouldThrow<IndexOutOfBoundsException> { text.findAnyPayloadOf(trie, Int.MAX_VALUE) }
            shouldThrow<IndexOutOfBoundsException> { text.findAnyPayloadOf(trie, -1) }
            unmatching.findAnyPayloadOf(trie).shouldBeNull()
            text.findAnyPayloadOf(trie).shouldNotBeNull() shouldBe Triple(1, "ore", 3)
            text.findAnyPayloadOf(trie, startIndex = 10).shouldNotBeNull() shouldBe Triple(22, "amet", 4)
            text.findAnyPayloadOf(trie, startIndex = 111).shouldBeNull()
        }

        test("can find the last match with index") {
            shouldThrow<IndexOutOfBoundsException> { text.findLastAnyOf(trie, Int.MAX_VALUE) }
            shouldThrow<IndexOutOfBoundsException> { text.findLastAnyOf(trie, -1) }
            unmatching.findLastAnyOf(trie).shouldBeNull()
            text.findLastAnyOf(trie) shouldBe (110 to "magna")
            text.findLastAnyOf(trie, startIndex = 10) shouldBe (110 to "magna")
            text.findLastAnyOf(trie, startIndex = 111).shouldBeNull()
        }

        test("can find the last payload with index") {
            shouldThrow<IndexOutOfBoundsException> { text.findLastAnyPayloadOf(trie, Int.MAX_VALUE) }
            shouldThrow<IndexOutOfBoundsException> { text.findLastAnyPayloadOf(trie, -1) }
            unmatching.findLastAnyPayloadOf(trie).shouldBeNull()
            text.findLastAnyPayloadOf(trie).shouldNotBeNull() shouldBe Triple(110, "magna", 5)
            text.findLastAnyPayloadOf(trie, startIndex = 10).shouldNotBeNull() shouldBe Triple(110, "magna", 5)
            text.findLastAnyPayloadOf(trie, startIndex = 111).shouldBeNull()
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

    context("Splitting text by payload") {
        test("validates the limit") {
            shouldThrow<IllegalArgumentException> { text.splitWithPayload(trie, limit = -1) }
        }

        test("tokenizes the text") {
            unmatching.splitWithPayload(trie).shouldBeSingleton { it shouldBe (unmatching to null) }
            text.splitWithPayload(trie).shouldContainExactly(
                "L" to null, "ore" to 3, "m " to null, "ipsum" to 5, " dolor sit " to null,
                "amet" to 4, ", consect" to null, "et" to 2,
                "ur adipiscing elit, sed do eiusmod tempor incididunt ut lab" to null, "ore" to 3, " " to null,
                "et" to 2, " dol" to null, "ore" to 3, " " to null, "magna" to 5, " aliqua." to null,
            )
        }

        test("respects the limit") {
            unmatching.splitWithPayload(trie, limit = 3).shouldBeSingleton { it shouldBe (unmatching to null) }
            text.splitWithPayload(trie, limit = 3).shouldContainExactly(
                "L" to null, "ore" to 3, "m " to null,
                "ipsum" to 5, " dolor sit " to null, "amet" to 4,
                text.substringAfter("amet") to null,
            )
        }

        test("consecutive matches still produce the empty in-between") {
            "ipsumamet".splitWithPayload(trie).shouldContainExactly(
                "" to null, "ipsum" to 5, "" to null, "amet" to 4,
            )
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

    context("Mapping over text with payloads") {
        test("does nothing on no matches") {
            unmatching
                .map(trie) { _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .mapNotNull(trie) {  _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .mapIndexed(trie) { _, _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .mapIndexedNotNull(trie) { _, _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .flatMap<Int, Unit>(trie) { _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
            unmatching
                .flatMapIndexed<Int, Unit>(trie) { _, _, _ -> fail("Should not be called!") }
                .shouldBeEmpty()
        }

        test("can map simple values") {
            text
                .map(trie) { _, payload -> payload.inc() }
                .shouldContainExactly(4, 6, 5, 3, 3, 4, 3, 4, 6)
        }

        test("can filter nulls in simple values") {
            text
                .mapNotNull(trie) { _, payload -> payload.takeIf { it > 2 } }
                .shouldContainExactly(3, 5, 4, 3, 3, 5)
        }

        test("can map values with index") {
            text
                .mapIndexed(trie) { index, _, payload -> index to payload.inc() }
                .shouldContainExactly(
                    1 to 4,
                    6 to 6,
                    22 to 5,
                    24 to 3,
                    35 to 3,
                    96 to 4,
                    100 to 3,
                    106 to 4,
                    110 to 6,
                )
        }

        test("can filter nulls in values with index") {
            text
                .mapIndexedNotNull(trie) { index, match, payload -> (index to payload).takeIf { match.length > 2 } }
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
                .flatMap(trie) { match, payload -> listOf(payload, match.first()) }
                .shouldContainExactly(3, 'o', 5, 'i', 4, 'a', 2, 'e', 2, 'e', 3, 'o', 2, 'e', 3, 'o', 5, 'm')
        }

        test("can flatmap values with index") {
            text
                .flatMapIndexed(trie) { index, match, _ -> listOf(index, match.first()) }
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
                .reformat(trie) { span, isMatch -> if (isMatch) "<$span>" else span.uppercase() }
                .shouldBe(
                    """
                        L<ore>M <ipsum> DOLOR SIT <amet>, CONSECT<et>UR ADIPISCING ELIT,
                        SED DO EIUSMOD TEMPOR INCIDIDUNT UT LAB<ore> <et> DOL<ore> <magna> ALIQUA.
                    """.trimIndent().replace("\n", " ")
                )
        }
    }

    context("Modifying text with payload") {
        test("makes no modifications if no matches") {
            unmatching.remove(trie, 2) shouldBe unmatching
            unmatching.replace(trie, 2, "#") shouldBe unmatching
            unmatching.replace(trie) { _, _ -> fail("should not be called") } shouldBe unmatching
        }

        test("can remove occurrences") {
            text.remove(trie) shouldBe """
                Lm  dolor sit , consectur adipiscing elit,
                sed do eiusmod tempor incididunt ut lab  dol  aliqua.
            """.trimIndent().replace("\n", " ")
        }

        test("can replace with constant") {
            text.replace(trie, 3, "***") shouldBe """
                L***m ipsum dolor sit amet, consectetur adipiscing elit,
                sed do eiusmod tempor incididunt ut lab*** et dol*** magna aliqua.
            """.trimIndent().replace("\n", " ")
        }

        test("can replace with function") {
            text.replace(trie) { _, payload -> "*".repeat(payload) } shouldBe """
                L***m ***** dolor sit ****, consect**ur adipiscing elit,
                sed do eiusmod tempor incididunt ut lab*** ** dol*** ***** aliqua.
            """.trimIndent().replace("\n", " ")
        }

        test("can reformat entire input") {
            text
                .reformatWithPayload(trie) { span, payload -> if (payload != null) "<$payload>" else span.uppercase() }
                .shouldBe(
                    """
                        L<3>M <5> DOLOR SIT <4>, CONSECT<2>UR ADIPISCING ELIT,
                        SED DO EIUSMOD TEMPOR INCIDIDUNT UT LAB<3> <2> DOL<3> <5> ALIQUA.
                    """.trimIndent().replace("\n", " ")
                )
        }
    }
})
