rootProject.name = "aho-corasikt"
includeBuild("gradle/build-logic")

pluginManagement {
    repositories {
        includeBuild("gradle/build-logic")
    }
}

plugins {
    id("conventions.project")
}
