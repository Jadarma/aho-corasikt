// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.util

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.CaseSensitivity.AsciiInsensitive
import io.github.jadarma.ahocorasikt.CaseSensitivity.SafeInsensitive
import io.github.jadarma.ahocorasikt.CaseSensitivity.Insensitive
import io.github.jadarma.ahocorasikt.CaseSensitivity.Sensitive
import io.github.jadarma.ahocorasikt.MatchType
import io.github.jadarma.ahocorasikt.MatchType.Unrestricted
import io.github.jadarma.ahocorasikt.MatchType.WhitespaceSeparated
import io.github.jadarma.ahocorasikt.MatchType.WholeWords

/** Converts this character using the [AsciiInsensitive] strategy. */
internal fun Char.toAsciiInsensitive(): Char =
    if (this in 'A'..'Z') lowercaseChar() else this

/** Converts this character using the [SafeInsensitive] strategy. */
internal fun Char.toSaneInsensitive(): Char {
    if (!isUpperCase() || isSurrogate()) return this
    val lower = lowercaseChar()
    return if (lower.uppercaseChar() == this) lower else this
}

/** Provides a character mapping function for the case sensitivity strategy. */
internal fun CaseSensitivity.asFunction(): (Char) -> Char = when (this) {
    Sensitive -> { char: Char -> char }
    AsciiInsensitive -> Char::toAsciiInsensitive
    SafeInsensitive -> Char::toSaneInsensitive
    Insensitive -> Char::lowercaseChar
}

/** Provides a character test function for the bounds matching strategy. */
internal fun MatchType.asFunction(): (Char) -> Boolean = when (this) {
    Unrestricted -> { _: Char -> true }
    WhitespaceSeparated -> Char::isWhitespace
    WholeWords -> { c -> !c.isLetter() }
}
