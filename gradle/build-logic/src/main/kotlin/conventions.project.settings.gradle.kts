plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        removeAll { true }
        mavenCentral()

        // Node & Yarn for KMP, workaround for:
        // https://youtrack.jetbrains.com/issue/KT-68533
        ivy("https://nodejs.org/dist/") {
            name = "Node Distributions at $url"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn Distributions at $url"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "Binaryen Distributions at $url"
            patternLayout { artifact("version_[revision]/[module]-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}
