package io.github.jadarma.ahocorasikt.internal.util

import io.github.jadarma.ahocorasikt.CaseSensitivity
import io.github.jadarma.ahocorasikt.CaseSensitivity.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.text.iterator

class TextUtilTest : FunSpec({

    context("Case-sensitivity") {

        val englishUp = "SPHINX OF BLACK QUARTS, JUDGE MY VOW!"
        val englishLow = "sphinx of black quarts, judge my vow!"

        val romanianUp = "ÎNCĂ VÂND GEM, WHISKY BEJ ȘI TEQUILA ROZ, PREȚ FIX."
        val romanianLow = "încă vând gem, whisky bej și tequila roz, preț fix."

        val polishUp = "PCHNĄĆ W TĘ ŁÓDŹ JEŻA LUB OŚM SKRZYŃ FIG."
        val polishLow = "pchnąć w tę łódź jeża lub ośm skrzyń fig."

        val bulgarianUp = "ПОД ЮЖНО ДЪРВО, ЦЪФТЯЩО В СИНЬО, БЯГАШЕ МАЛКО ПУХКАВО ЗАЙЧЕ."
        val bulgarianLow = "под южно дърво, цъфтящо в синьо, бягаше малко пухкаво зайче."

        val arabic = "نص حكيم له سر قاطع وذو شأن عظيم مكتوب على ثوب أخضر ومغلف بجلد أزرق"
        // Arabic has no casing.

        val htmlRegexUp = "Z̴̜̮̾̌A̶͎͋̓L̷̨̺̯͊̅̀G̵͉̠̈͌̒O̵̡̽ ̶̼̖̃͗̌H̸̼͍̀̉͐E̴͈̽̇̕ ̷̦͎̔̔͝C̸̥͇͐͌̑Ọ̶͕͙̌́M̴̢͒Ę̷̘̲̄͋̓S̶̙̐ͅ"
        val htmlRegexLow = "z̴̜̮̾̌a̶͎͋̓l̷̨̺̯͊̅̀g̵͉̠̈͌̒o̵̡̽ ̶̼̖̃͗̌h̸̼͍̀̉͐e̴͈̽̇̕ ̷̦͎̔̔͝c̸̥͇͐͌̑ọ̶͕͙̌́m̴̢͒ę̷̘̲̄͋̓s̶̙̐ͅ"

        val sarcasm = "WoW, Ai Is ToTaLly GoNnA ReVoLuTiOnIzE tHe InDuStRy!"
        val sarcasmLow = "wow, ai is totally gonna revolutionize the industry!"

        context(Sensitive.name) {
            test("Preserves symbols exactly") {
                englishUp.withSensitivity(Sensitive) shouldBe englishUp
                englishLow.withSensitivity(Sensitive) shouldBe englishLow
                romanianUp.withSensitivity(Sensitive) shouldBe romanianUp
                romanianLow.withSensitivity(Sensitive) shouldBe romanianLow
                polishUp.withSensitivity(Sensitive) shouldBe polishUp
                polishLow.withSensitivity(Sensitive) shouldBe polishLow
                bulgarianUp.withSensitivity(Sensitive) shouldBe bulgarianUp
                bulgarianLow.withSensitivity(Sensitive) shouldBe bulgarianLow
                arabic.withSensitivity(Sensitive) shouldBe arabic
                htmlRegexUp.withSensitivity(Sensitive) shouldBe htmlRegexUp
                sarcasm.withSensitivity(Sensitive) shouldBe sarcasm
            }
        }

        context(AsciiInsensitive.name) {
            test("Converts ASCII to lowercase.") {
                englishUp.withSensitivity(AsciiInsensitive) shouldBe englishLow
                sarcasm.withSensitivity(AsciiInsensitive) shouldBe sarcasmLow
            }
            test("Ignores unicode") {
                romanianUp
                    .withSensitivity(AsciiInsensitive)
                    .shouldBe("ÎncĂ vÂnd gem, whisky bej Și tequila roz, preȚ fix.")
                polishUp
                    .withSensitivity(AsciiInsensitive)
                    .shouldBe("pchnĄĆ w tĘ ŁÓdŹ jeŻa lub oŚm skrzyŃ fig.")
                bulgarianUp.withSensitivity(AsciiInsensitive) shouldBe bulgarianUp
                arabic.withSensitivity(AsciiInsensitive) shouldBe arabic
                htmlRegexUp.withSensitivity(AsciiInsensitive).shouldBe(htmlRegexLow)
            }
        }

        context(SafeInsensitive.name) {
            test("Converts unicode") {
                englishUp.withSensitivity(SafeInsensitive) shouldBe englishLow
                romanianUp.withSensitivity(SafeInsensitive) shouldBe romanianLow
                polishUp.withSensitivity(SafeInsensitive) shouldBe polishLow
                bulgarianUp.withSensitivity(SafeInsensitive) shouldBe bulgarianLow
                arabic.withSensitivity(SafeInsensitive) shouldBe arabic
                htmlRegexUp.withSensitivity(SafeInsensitive) shouldBe htmlRegexLow
                sarcasm.withSensitivity(SafeInsensitive) shouldBe sarcasmLow
            }
            test("Ignores problematic characters") {
                val example = "I *SHOULD* Convert Î, Σ, and I, but NOT K, İ, and İ!"
                val expected = "i *should* convert î, σ, and i, but not K, İ, and İ!"
                example.withSensitivity(SafeInsensitive) shouldBe expected
            }
        }

        context(Insensitive.name) {
            test("Converts unicode") {
                englishUp.withSensitivity(SafeInsensitive) shouldBe englishLow
                romanianUp.withSensitivity(SafeInsensitive) shouldBe romanianLow
                polishUp.withSensitivity(SafeInsensitive) shouldBe polishLow
                bulgarianUp.withSensitivity(SafeInsensitive) shouldBe bulgarianLow
                arabic.withSensitivity(SafeInsensitive) shouldBe arabic
                htmlRegexUp.withSensitivity(SafeInsensitive) shouldBe htmlRegexLow
                sarcasm.withSensitivity(SafeInsensitive) shouldBe sarcasmLow
            }
            test("Also converts problematic characters") {
                val example = "I *SHOULD* Convert Î, Σ, and I, and ALSO K, İ, and İ!"
                val expected = "i *should* convert î, σ, and i, and also k, i, and i!"
                example.withSensitivity(Insensitive) shouldBe expected
            }
        }
    }
})

/**
 * Maps this character sequence to another of equal length, converting each individual character according to the
 * specified [sensitivity].
 */
private fun CharSequence.withSensitivity(sensitivity: CaseSensitivity): CharSequence {
    val mapper = sensitivity.asFunction()
    return buildString(capacity = length) {
        for (char in this@withSensitivity) {
            append(mapper(char))
        }
    }
}
