package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.test.ofVariants
import io.github.jadarma.ahocorasikt.test.shouldMatchExactly
import io.kotest.core.spec.style.FunSpec

class CaseSensitivityTest : FunSpec({

    val text = """
        This text is normal and contains just letters.
        THIS TEXT IS SCREAMING AT YOÜ!
        ThIs TeXt Is SaRcAsTiC LoL 🙄
        Acest.Text.Este.În.ROMÂNĂ!
        Funny dots over THİS word.
        The temperature outside is 300K, but you can also say 300k in a pinch.
        How about combined graphemes: 👨‍👩‍👧‍👦? 
    """.trimIndent()

    val words = setOf(
        "This",
        "TEXT",
        "SaRcAsTiC",
        "română",
        "THİS",
        "300K",
        "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66", // 👨‍👩‍👧‍👦
    )

    context(CaseSensitivity.Sensitive.name) {
        val trie = Trie.ofVariants(words, caseSensitivity = CaseSensitivity.Sensitive)

        test("Finds keywords exactly as given") {

            val expected = listOf(
                0..3 to "This",
                52..55 to "TEXT",
                91..99 to "SaRcAsTiC",
                151..154 to "THİS",
                189..192 to "300K",
                263..273 to "👨‍👩‍👧‍👦",
            )

            trie.shouldMatchExactly(text, expected)
        }
    }

    context(CaseSensitivity.AsciiInsensitive.name) {
        val trie = Trie.ofVariants(words, caseSensitivity = CaseSensitivity.AsciiInsensitive)

        test("Finds all casing variations of ASCII letters") {
            val expected = listOf(
                0..3 to "This",
                5..8 to "text",
                47..50 to "THIS",
                52..55 to "TEXT",
                78..81 to "ThIs",
                83..86 to "TeXt",
                91..99 to "SaRcAsTiC",
                114..117 to "Text",
                151..154 to "THİS",
                189..192 to "300K",
                263..273 to "👨‍👩‍👧‍👦",
            )

            trie.shouldMatchExactly(text, expected)
        }
    }

    context(CaseSensitivity.SafeInsensitive.name) {
        val trie = Trie.ofVariants(words, caseSensitivity = CaseSensitivity.SafeInsensitive)

        test("Handles all safe unicode case mappings") {
            val expected = listOf(
                0..3 to "This",
                5..8 to "text",
                47..50 to "THIS",
                52..55 to "TEXT",
                78..81 to "ThIs",
                83..86 to "TeXt",
                91..99 to "SaRcAsTiC",
                114..117 to "Text",
                127..132 to "ROMÂNĂ", // <- This is a valid mapping outside of ASCII.
                151..154 to "THİS",
                189..192 to "300K",
                263..273 to "👨‍👩‍👧‍👦",
            )

            trie.shouldMatchExactly(text, expected)
        }
    }

    context(CaseSensitivity.Insensitive.name) {
        val trie = Trie.ofVariants(words, caseSensitivity = CaseSensitivity.Insensitive)

        test("Is the most relaxed matcher.") {
            val expected = listOf(
                0..3 to "This",
                5..8 to "text",
                47..50 to "THIS",
                52..55 to "TEXT",
                78..81 to "ThIs",
                83..86 to "TeXt",
                91..99 to "SaRcAsTiC",
                114..117 to "Text",
                127..132 to "ROMÂNĂ",
                151..154 to "THİS",
                189..192 to "300K",
                216..219 to "300k", // <- Technically still represents Kelvin, but not in all contexts.
                263..273 to "👨‍👩‍👧‍👦",
            )

            trie.shouldMatchExactly(text, expected)
        }
    }
})
