plugins {
    id("com.diffplug.spotless") version "8.10.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.22" apply false
    id("net.ltgt.errorprone") version "5.1.0" apply false
    id("xyz.jpenilla.run-paper") version "3.1.0" apply false
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
