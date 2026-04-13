import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

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
}
