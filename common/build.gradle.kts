import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

base {
    archivesName = "unplugged-afk-common"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
}

dependencies {
    // Dependencies
    compileOnly(libs.netty.buffer)
    compileOnly(libs.netty.codec.base)
    compileOnly(libs.netty.transport)

    // Annotations
    compileOnly(libs.jspecify)
    compileOnly(libs.jetbrains.annotations)

    // Linting
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

spotless {
    java {
        target("src/main/java/**/*.java")

        palantirJavaFormat().formatJavadoc(true)
        removeUnusedImports()
        forbidWildcardImports()
        importOrder("", "javax|java", "\\#")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Werror")
    options.errorprone {
        disableWarningsInGeneratedCode = true

        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
    }
}
