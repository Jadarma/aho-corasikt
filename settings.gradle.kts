rootProject.name = "aho-corasikt"
includeBuild("gradle/build-logic")

pluginManagement {
    repositories {
        removeAll { true }
        includeBuild("gradle/build-logic")
    }
}

plugins {
    id("conventions.project")
}
