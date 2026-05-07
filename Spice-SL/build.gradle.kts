import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

ktlint {
    version = "1.8.0"
    android = true
    ignoreFailures = false

    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.HTML)
    }
}

// 1. Find the Git Root dynamically
val gitRootPath = providers
    .exec {
        commandLine("git", "rev-parse", "--show-toplevel")
    }.standardOutput.asText
    .map { it.trim() }

// Define the task at the root level
tasks.register<Copy>("installGitHooks") {
    description = "Copies git hooks from /scripts to .git/hooks"
    group = "git hooks"

    from(layout.projectDirectory.dir("scripts/pre-commit"))
    into(gitRootPath.map { file("$it/.git/hooks") })

    // Modern way to set executable permissions (rwxr-xr-x)
    eachFile {
        permissions {
            unix(0b111101101) // 755
        }
    }

    outputs.upToDateWhen { false }
}

project(":app") {
    afterEvaluate {
        tasks.named("preBuild") {
            dependsOn(rootProject.tasks.named("installGitHooks"))
        }
    }
}
