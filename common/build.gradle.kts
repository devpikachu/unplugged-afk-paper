import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

base {
    archivesName = "unplugged-afk-common"
}

dependencies {
    compileOnly(libs.jspecify)
    compileOnly(libs.jetbrains.annotations)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Werror")
    options.errorprone {
        disableWarningsInGeneratedCode = true

        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
    }
}

spotless {
    java {
        target("src/main/java/**/*.java")

        eclipse().configFile(rootProject.file("eclipse-formatter.properties"))
        removeUnusedImports()
        forbidWildcardImports()
        importOrder("", "javax|java", "\\#")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
