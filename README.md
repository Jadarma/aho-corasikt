# Aho-CorasiKt

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-%237F52FF.svg?style=flat-square&logo=kotlin&logoColor=%237F52FF&labelColor=%23181717)](https://kotlinlang.org/)
[![Documentation](https://img.shields.io/badge/Dokka-purple?style=flat-square&logo=bookstack&logoColor=7F52FF&label=Docs&labelColor=181717&color=7F52FF)](https://www.javadoc.io/doc/io.github.jadarma.ahocorasikt/aho-corasikt/latest/index.html)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.jadarma.ahocorasikt/aho-corasikt?strategy=highestVersion&style=flat-square&logo=apachemaven&logoColor=C71A36&label=Maven%20Central&labelColor=181717&color=C71A36)](https://central.sonatype.com/namespace/io.github.jadarma.ahocorasikt)
[![GitHub License](https://img.shields.io/github/license/Jadarma/aho-corasikt?style=flat-square&logo=opensourceinitiative&logoColor=3DA639&label=License&labelColor=181717&color=3DA639)](https://github.com/Jadarma/aho-corasikt/blob/main/LICENSE.md)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/Jadarma/aho-corasikt/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=2088FF&label=Build&labelColor=181717&color=2088FF)](https://github.com/Jadarma/aho-corasikt/actions/workflows/build.yml)

Kotlin Multiplatform implementation of the [Aho-Corasick](https://cr.yp.to/bib/1975/aho.pdf) algorithm for multiple
string pattern matching.

## ✨ Features

- **Multiplatform**: Pure Kotlin Stdlib implementation, available for all supported targets.
- **Lazy**: Matches are emitted as sequences; only parse what you need.
- **Familiar API**: Based on interfaces and extension functions that follow Kotlin's standard conventions.
- **Customizable**: Can specify case sensitivity, match style, overlap resolution, and custom payloads.

## 🛠️ Installation

Add the
[`io.github.jadarma.ahocorasikt:aho-corasikt`](https://central.sonatype.com/artifact/io.github.jadarma.ahocorasikt/aho-corasikt)
package to your dependencies:

```toml
# gradle/libs.versions.toml
[versions]
ahoCorasiKt = "0.1.0" # https://central.sonatype.com/artifact/io.github.jadarma.ahocorasikt/aho-corasikt/versions

[libraries]
ahoCorasiKt = { module = "io.github.jadarma.ahocorasikt:aho-corasikt", version.ref = "ahoCorasiKt" }
```
```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        val commonMain by getting {
            dependenceis {
                implementation(libs.ahoCorasiKt)
            }
        }
    }
}
```

## 📚 Usage Guide

### Building a Trie

The [Wikipedia example](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm) trie can be built like so:

```kotlin
val trie = trieOf("a", "ab", "bab", "bca", "c", "caa")
"abccab".find(trie).joinToString(separator = "\n").let(::println)
// (0, a), (0, ab), (2, c). (3, c), (4, a), (4, ab)
```

If not all words are already available, they can be added one at a time using the builder:

```kotlin
val builder = Trie.builder()
for(word in source()) {
    builder.add(word)
}
val trie = builder.build()
```

A convenience builder function that wraps the above behavior is also available:

```kotlin
val trie = buildTrie { 
    for(word in source()) {
        add(word)
    }
}
```

All builder variants have optional configuration parameters.

### Case-Sensitivity

Tries can be configured to handle case sensitivity in a specific way by passing a builder argument:
- `Sensitive`: Characters must match exactly. The default unless specified otherwise.
- `AsciiInsensitive`: The letters `A-Z` are treated as `a-z`, others must match exactly.
- `SafeInsensitive`: All Unicode codepoints that do not require a surrogate (encodable as a single `Char`), are treated
  as their lowercase variant, but *only if* the uppercase of its lowercase variant maps back to the original character.
  This helps avoid ambiguations, but might not be suitable for all cases.
- `Insensitive`: Every input character is treated as its `Char.lowercaseChar()` value.
  Note this still does not support multi-character case mapping, because that would misalign the match ranges.
  For those cases, it is better to add both variants as separate keywords in the trie.

Note the words do not have to be lowercased when added to the builder;
the case mapping is done automatically on insertion.

Also note that case sensitivity alone is not enough to guarantee full Unicode support.
Some characters may be encoded as a single codepoint, or a composition of multiple.
As such, it is recommended you [normalize](https://unicode.org/reports/tr15) text _(and the trie keywords)_ beforehand.

### MatchType

Tries can be configured to ignore matches based on their surrounding in the original string:

- `Unrestricted`: All matches are kept. The default unless specified otherwise.
- `WhitespaceSeparated`: Matches are ignored unless preceded and followed by _(any)_ whitespace or string boundaries.
- `WholeWords`: Matches are ignored unless preceded and followed by non-letter symbols or string boundaries.

### Overlap

Tries can be configured to ensure no emitted matches overlap:

- `Allowed`: All matches are kept, regardless of overlap. The default unless specified otherwise.
- `Leftmost`: The match that starts earlier wins; in a tie, the longer one wins.
- `Longest`: The longer match wins; in a tie, the one that starts earlier wins.

When using overlap mode, matches are only emitted when they are guaranteed not to overlap any potential future match.
The matches are buffered as candidates until confirmed
_(the process is still lazy; the entire input does not need to be parsed first)_.

### Custom Payloads

A `PayloadTrie` variant exists that requires all words to be associated to a non-null value:

```kotlin
enum class Keyword { Declaration, Modifier, Operator }
val kotlin = buildTrie<Keyword> {
    addAll(listOf("class", "var", "val", "fun"), Declaration)
    addAll(listOf("public", "internal", "private"), Modifier)
    addAll(listOf("=", "+", "-", "in"), Operator)
}
```

These payloads can be emitted together with the matches.
See extension functions for details.

## 🧩 Use-case Examples

### Censoring

```kotlin
val secrets = trieOf("hunter2", "p@ssw0rd")
val text = "My password is hunter2."
text.replace(secrets, "***")
// My password is ***.
text.replace(secrets) { it.first() + "*".repeat(it.lastIndex) }
// My password is h******.
```

### Spell Checker

```kotlin
val words = trieOf(englishWords)
"definitely" in words // true
"definately" in words // false
```

### Markup, Highlighting, Formatting

```kotlin
val text = "Everyone please, just stay calm! He said calmingly."
val emphasis = trieOf("please", "calm", matchType = MatchType.OnlyWholeWords)
text.reformat(ephasis) { span, isMatch ->
    val tag = if(isMatch) "b" else "i"
    val content = if(isMatch) span.uppercase() else span 
    "<$tag>$content</$tag>"
}
// <i>Everyone </i><b>PLEASE</b><i>, just stay </i><b>CALM</b><i>! He said calmingly.</i>
```

### With Payload

```kotlin
enum class PartOfSpeech { Noun, Adjective, Adverb, Verb }
val trie = trieOf<PartOfSpeech>(
    mapOf(
        "best" to Adjective,
        "worst" to Adjective,
        "well" to Adverb,
        "fails" to Verb,
        // ...
    )
)
val adjectives = "This is one of the worst examples for one of the best features."
    .findWithPayload(trie)
    .filter { it.third == Adjective }
    .map { it.second }
    .toSet()
// [ "worst", "best" ]
```

***(And more…)***

## ⚖️ License

    Copyright 2026 Dan Cîmpianu
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
      https://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
