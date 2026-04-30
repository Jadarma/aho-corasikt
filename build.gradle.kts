import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotest)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
}

group = "io.github.jadarma.ahocorasikt"
version = "0.1.0"

kotlin {
    jvm()

    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(11)
        vendor = JvmVendorSpec.ADOPTIUM
    }

    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_3
        apiVersion = KotlinVersion.KOTLIN_2_1
        allWarningsAsErrors = true
        explicitApi()
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.engine)
                implementation(libs.kotest.assertions)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.junit)
            }
        }
    }
}

//region Detekt -------------------------------------------------------------------------------------------------------
detekt {
    buildUponDefaultConfig = false
    parallel = true
}

tasks {
    withType<Detekt> {
        reports {
            html.required = true
            sarif.required = true
            markdown.required = false
            checkstyle.required = false
        }
        // See https://github.com/detekt/detekt/issues/5611
        exclude { it.file.invariantSeparatorsPath.contains("build/generated/")}
    }

    val detektMain: Detekt by named<Detekt>("detektCommonMainSourceSet") {
        baseline = rootDir.resolve("gradle/detekt/baseline-main.xml")
        config.setFrom(
            rootDir.resolve("gradle/detekt/detekt.yml"),
        )
    }
    val detektTest: Detekt by named<Detekt>("detektCommonTestSourceSet") {
        baseline = rootDir.resolve("gradle/detekt/baseline-test.xml")
        config.setFrom(
            rootDir.resolve("gradle/detekt/detekt.yml"),
            rootDir.resolve("gradle/detekt/detekt-test.yml"),
        )
    }
    val detekt: Detekt by named<Detekt>("detekt") {
        exclude { true } // Force no source, this is an aggregate task now.
        dependsOn(detektMain, detektTest)
    }
}
//endregion

//region Kotest -------------------------------------------------------------------------------------------------------
tasks.withType<Test>().configureEach {
    if (name == "jvmTest") useJUnitPlatform()
    failOnNoDiscoveredTests = false

    // Don't cache tests, make them run again every time.
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }

    // Pass along system properties for Kotest.
    systemProperties = System.getProperties()
        .asIterable()
        .filter { it.key.toString().startsWith("kotest.") }
        .associate { it.key.toString() to it.value }

    systemProperty("kotest.framework.config.fqn", "$group.test.TestConfig")
}
//endregion

//region Kover --------------------------------------------------------------------------------------------------------
kover {
    reports {
        total {
            html {
                onCheck = true
                charset = "UTF-8"
            }
            binary {
                onCheck = true
            }
        }
        filters {
            includes {
                classes("$group.*")
            }
            excludes {
                classes($$"*.*$DefaultImpls")
            }
        }
    }
}
//endregion
