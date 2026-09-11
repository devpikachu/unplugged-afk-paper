import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("com.gradleup.shadow")
    id("net.ltgt.errorprone")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

base {
    archivesName = "unplugged-afk-velocity"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
}

dependencies {
    // Velocity
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    // Common
    implementation(project(":common"))

    // Integrations
    compileOnly(libs.miniplaceholders.api)

    // Dependencies
    compileOnly(libs.netty.buffer)
    compileOnly(libs.netty.codec.base)
    compileOnly(libs.netty.handler)
    compileOnly(libs.netty.transport)
    implementation(libs.snakeyaml)

    // Annotations
    compileOnly(libs.jspecify)
    compileOnly(libs.jetbrains.annotations)

    // Linting
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier = ""

        relocate("de.exlll.configlib", "dev.detpikachu.unpluggedafk.libs.configlib")
        relocate("org.snakeyaml.engine", "dev.detpikachu.unpluggedafk.libs.snakeyaml.engine")
        relocate("org.yaml.snakeyaml", "dev.detpikachu.unpluggedafk.velocity.libs.snakeyaml")
    }
}

val templateProps = mapOf("version" to project.version.toString())
val generateTemplates = tasks.register<Copy>("generateTemplates") {
    inputs.properties(templateProps)

    from(file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/templates"))
    expand(templateProps)
}

sourceSets.main {
    java.srcDir(generateTemplates.map { it.outputs })
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
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
    options.errorprone {
        disableWarningsInGeneratedCode = true

        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
    }
}
