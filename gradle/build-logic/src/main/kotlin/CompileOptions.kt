import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * Variables to use for various compilation configurations, to allow them to be referenced from precompiled scripts and
 * be changed globally.
 */
object CompileOptions {

    object Package {
        const val GROUP_ID: String = "io.github.jadarma.ahocorasikt"
        const val ARTIFACT_ID: String = "aho-corasikt"
        const val CURRENT: String = "0.1.0" // Last released version.
        const val NEXT: String = "0.2.0" // Current snapshot.
    }

    object Java {
        val languageVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)
        val jvmVendor: JvmVendorSpec = JvmVendorSpec.ADOPTIUM
    }

    object Kotlin {
        val languageVersion: KotlinVersion = KotlinVersion.KOTLIN_2_4
        val apiVersion: KotlinVersion = KotlinVersion.KOTLIN_2_2
        val jvmTarget: JvmTarget = JvmTarget.fromTarget(Java.languageVersion.asInt().toString())
    }
}
