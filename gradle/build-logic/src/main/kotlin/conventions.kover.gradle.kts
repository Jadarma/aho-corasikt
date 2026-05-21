import CompileOptions.Package.GROUP_ID

plugins {
    id("org.jetbrains.kotlinx.kover")
}

kover {
    reports {
        total {
            html {
                onCheck = true
                charset = "UTF-8"
            }
            binary {
                onCheck = true
            }
        }
        filters {
            includes {
                classes("$GROUP_ID.*")
            }
            excludes {
                packages("$GROUP_ID.benchmark")
                classes($$"*.*$DefaultImpls")
            }
        }
    }
}
