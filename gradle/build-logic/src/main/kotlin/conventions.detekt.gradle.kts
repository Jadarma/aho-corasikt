import dev.detekt.gradle.Detekt
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

plugins {
    kotlin("multiplatform")
    id("dev.detekt")
}

detekt {
    buildUponDefaultConfig = false
    parallel = true
}

tasks {
    withType<Detekt>().configureEach {
        reports {
            html.required = true
            sarif.required = true
            markdown.required = false
            checkstyle.required = false
        }
        // See https://github.com/detekt/detekt/issues/5611
        exclude { it.file.invariantSeparatorsPath.contains("build/generated/") }
    }

    val detektMain: Detekt = getByName<Detekt>("detektCommonMainSourceSet") {
        baseline = rootDir.resolve("gradle/detekt/baseline/baseline-main.xml")
        config.setFrom(rootDir.resolve("gradle/detekt/detekt.yml"))
    }
    val detektTest: Detekt = getByName<Detekt>("detektCommonTestSourceSet") {
        baseline = rootDir.resolve("gradle/detekt/baseline/baseline-test.xml")
        config.setFrom(
            rootDir.resolve("gradle/detekt/detekt.yml"),
            rootDir.resolve("gradle/detekt/detekt-test.yml"),
        )
    }
    named<Detekt>("detekt") {
        exclude { true } // Force no source, this is an aggregate task now.
        dependsOn(detektMain, detektTest)
    }
}
