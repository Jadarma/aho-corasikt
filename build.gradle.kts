plugins {
    id("conventions.kotlin")
    id("conventions.detekt")
    id("conventions.kotest")
    id("conventions.kover")
    id("conventions.dokka")
    id("conventions.publishing")
    id("conventions.benchmark")
}

group = CompileOptions.Package.GROUP_ID
version = project.buildVersion.get()
