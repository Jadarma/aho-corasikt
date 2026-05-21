import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    val docVersion = project.buildVersion.get()
    moduleName = "Aho-CorasiKt"
    moduleVersion = docVersion.toString()

    dokkaSourceSets.configureEach {
        if(docVersion.isRelease) {
            sourceLink {
                remoteUrl("https://github.com/Jadarma/aho-corasikt/tree/v$version")
                remoteLineSuffix = "#L"
            }
        }

        reportUndocumented = true
        documentedVisibilities = setOf(VisibilityModifier.Public)
        enableJdkDocumentationLink = false
        enableKotlinStdLibDocumentationLink = false
        jdkVersion = CompileOptions.Java.languageVersion.asInt()
        languageVersion = CompileOptions.Kotlin.languageVersion.version
        apiVersion = CompileOptions.Kotlin.apiVersion.version
    }

    dokkaPublications.html {
        offlineMode = true
        suppressObviousFunctions = true
        suppressInheritedMembers = true
    }

    pluginsConfiguration.html {
        separateInheritedMembers = true
        footerMessage = "Copyright © 2026 Dan Cîmpianu"
    }

    dokkaGeneratorIsolation = ClassLoaderIsolation()
}
