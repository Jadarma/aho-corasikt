import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    // Targets
    jvm()

    // Native
    linuxX64()
    linuxArm64()
    macosArm64()
    mingwX64()
    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    // Web
    js {
        nodejs()
        browser { testTask { useKarma { useFirefoxHeadless() } } }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser { testTask { useKarma { useFirefoxHeadless() } } }
    }

    jvmToolchain {
        languageVersion = CompileOptions.Java.languageVersion
        vendor = CompileOptions.Java.jvmVendor
    }

    compilerOptions {
        languageVersion = CompileOptions.Kotlin.languageVersion
        apiVersion = CompileOptions.Kotlin.apiVersion
        allWarningsAsErrors = true
        explicitApi()
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }

    applyDefaultHierarchyTemplate()

    /**
     * On Linux, do not link any shared libraries that aren't needed.
     * This is especially useful for NixOS.
     * Also see: https://youtrack.jetbrains.com/projects/KT/issues/KT-55643
     */
    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.name.startsWith("linux") }
        .forEach { target ->
            target.binaries.all {
                linkerOpts("-Wl,--as-needed")
            }
        }
}
