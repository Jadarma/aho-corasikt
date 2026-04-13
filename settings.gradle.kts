rootProject.name = "aho-corasikt"

pluginManagement {
    repositories {
        removeAll { true }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        removeAll { true }
        mavenCentral()
    }
}
