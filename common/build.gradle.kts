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
    implementation(libs.configlib.core)
    implementation(libs.configlib.yaml)
    implementation(libs.snakeyaml.engine)
    compileOnly(libs.netty.buffer)
    compileOnly(libs.netty.codec.base)
    compileOnly(libs.netty.transport)
    compileOnly(libs.slf4j.api)

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

        palantirJavaFormat(libs.versions.palantir.java.format.get()).formatJavadoc(true)
        removeUnusedImports()
        forbidWildcardImports()
        importOrder("", "javax|java", "\\#")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    options.errorprone {
        disableWarningsInGeneratedCode = true

        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
    }
}
