repositories {
    mavenCentral()
    val githubToken = System.getenv("GITHUB_TOKEN")
    if (githubToken.isNullOrEmpty()) {
        maven {
            name = "external-mirror-github-navikt"
            url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        }
        maven {
            name = "maven-releases"
            url = uri("https://repo.adeo.no/repository/maven-releases/")
        }
        maven {
            name = "maven-snapshots"
            url = uri("https://repo.adeo.no/repository/maven-snapshots/")
        }
        maven {
            name = "NAV internal Nexus"
            url = uri("https://repo.adeo.no/repository/maven-public")
        }
    } else {
        maven {
            name = "github-package-registry-navikt"
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "token"
                password = githubToken
            }
        }
    }
    maven { url = uri("https://jitpack.io") }
}
