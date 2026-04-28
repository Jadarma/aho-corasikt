// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt

/** Determines how a search [Trie] will treat case sensitivity in characters. */
public enum class CaseSensitivity {

    /** The characters must match exactly. */
    Sensitive,

    /**
     * The latin letters `A-Z` are treated as `a-z`.
     * All other characters use case-sensitive matching.
     */
    AsciiInsensitive,

    /**
     * Offers partial support for case-insensitivity for Unicode strings.
     * Although not perfect, it uses sane rules to stay deterministic and support most characters and avoid confusion.
     *
     * Surrogates, already lowercased letters, and other symbols match as-is.
     * Characters that are considered to be uppercase _(see [Char.isUpperCase])_ will be matched as case-insensitive
     * *only if* the uppercase of its lowercase variant maps back to the original character.
     * Note this uses the one-to-one Unicode mapping rule of the invariant locale.
     * Some characters use one-to-many mapping, and are *ignored* because they would mess with the indexing of strings.
     *
     * Here are some examples and edge-cases:
     * - `Î` _(`\u00CE`)_ can match either itself or `î` _(`\u00EE`)_, because they are converted unambiguously.
     * - `K` _(`\u212A`)_ will only match against itself, because even though it gets lowercased to a single character
     *   `k` _(`\u006B`)_, when uppercased back it yield the normal letter `K` _(`\u004B`)_.
     * - `Σ` _(`\u03A3`)_ will match against itself or `σ` _(`\u03C3`)_, but _NOT_ against `ς` _(`\u03C2`)_, because it
     *   depends on relative position inside the word, and the engine cannot account for it.
     * - `İ` _(`\u0130`)_ will only match against itself, because its lowercase variant uses two codepoints:
     *   `i̇` _(`\u0069\u0307`)_.
     * - `ß` _(`\u00DF`)_ will match against itself because it is already lowercase.
     *   It doesn't matter that its uppercase would be two symbols _(`SS`/`\u0053\u0053`)_.
     */
    SafeInsensitive,

    /**
     * ***Use with care!***
     *
     * Behaves like [SafeInsensitive], but does not enforce the rule of reversible mapping.
     * The one-to-one character rule is still needed to preserve indexes.
     * All input characters can match against either themselves or their [Char.lowercaseChar] value.
     */
    Insensitive;
}

/** Determines when matching keywords should be emitted. */
public enum class MatchType {

    /** Any valid keyword match is emitted. */
    Unrestricted,

    /** A keyword match is only emitted if the characters immediately adjacent to it are whitespace. */
    WhitespaceSeparated,

    /** A keyword match is only emitted if the characters immediately adjacent to it are not letters. */
    WholeWords;
}

/** Determines how to handle keyword matches whose ranges overlap. */
public enum class Overlap {

    /** All keyword matches are emitted, regardless of overlap. */
    Ignored,

    /** If two matches overlap, the one with the lower starting index wins. */
    Leftmost,

    /** If two matches overlap, the longer one wins. If it's a tie, the leftmost wins. */
    Longest;
}
