plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.paperweight.userdev) apply false
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.run.paper) apply false
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")

        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}
