pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Fallback mirror of maven.google.com, repo1.maven.org and plugins.gradle.org.
        // Only used when the official repositories above fail to answer a request.
        maven {
            name = "mirror"
            url = uri("https://maven.myket.ir")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "mirror"
            url = uri("https://maven.myket.ir")
        }
    }
}

rootProject.name = "OP Player"
include(":app")
