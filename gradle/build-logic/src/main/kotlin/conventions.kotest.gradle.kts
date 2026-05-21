plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
    id("io.kotest")
    id("com.goncalossilva.resources")
}

kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.engine)
                implementation(libs.kotest.assertions)
                implementation(libs.testResources)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.junit)
            }
        }
    }
}

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
