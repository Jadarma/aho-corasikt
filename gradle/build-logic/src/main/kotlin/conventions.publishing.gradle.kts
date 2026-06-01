import CompileOptions.Package.ARTIFACT_ID
import CompileOptions.Package.GROUP_ID
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    if(System.getenv("PUBLISHING_ENABLED") == "true") {
        publishToMavenCentral(automaticRelease = false)
        signAllPublications()
    }

    configure(
        KotlinMultiplatform(
            sourcesJar = SourcesJar.Sources(),
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
        )
    )

    coordinates(
        groupId = GROUP_ID,
        artifactId = ARTIFACT_ID,
        version = buildVersion.get().toString(),
    )

    pom {
        name = "Aho-CorasiKt"
        description = "Kotlin Multiplatform implementation of the Aho-Corasick algorithm for multiple string pattern matching."
        url = "https://jadarma.github.com/Jadarma/aho-corasikt"
        inceptionYear = "2026"

        scm {
            url = "https://jadarma.github.com/Jadarma/aho-corasikt"
            connection = "scm:git:git://github.com/Jadarma/aho-corasikt.git"
            developerConnection = "scm:git:ssh://github.com/Jadarma/aho-corasikt.git"
        }

        developers {
            developer {
                id = "Jadarma"
                name = "Dan Cîmpianu"
                url = "https://github.com/Jadarma"
                email = "dancristiancimpianu@gmail.com"
            }
        }

        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
    }
}
