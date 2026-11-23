plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark")
}

kotlin {
    val benchmarkTargets = setOf("jvm", "macosArm64", "linuxX64")

    sourceSets {
        val commonBenchmark = create("commonBenchmark") {
            dependencies {
                implementation(libs.kotlinx.benchmark.runtime)
            }
        }

        targets.matching { it.name in benchmarkTargets }.configureEach {
            compilations {
                create("benchmark") {
                    associateWith(getByName("main"))
                    associateWith(getByName("test"))
                    defaultSourceSet.dependsOn(commonBenchmark)
                }
            }
        }
    }
}

benchmark {
    configurations.getByName("main") {
        reportFormat = "text"
        warmups = 5
        iterations = 5
        iterationTime = 5
        iterationTimeUnit = "s"
    }
    targets {
        register("jvmBenchmark")
        register("macosArm64Benchmark")
        register("linuxX64Benchmark")
    }
}
