import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

val velocityVersion: String = project.property("velocityVersion") as String
val javaVersion: String = project.property("javaVersion") as String
val errorproneVersion: String = project.property("errorproneVersion") as String
val nullawayVersion: String = project.property("nullawayVersion") as String
val jspecifyVersion: String = project.property("jspecifyVersion") as String
val jetbrainsAnnotationsVersion: String = project.property("jetbrainsAnnotationsVersion") as String

base {
    archivesName = "unplugged-afk-velocity"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")

    compileOnly("org.jspecify:jspecify:$jspecifyVersion")
    compileOnly("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")

    errorprone("com.google.errorprone:error_prone_core:$errorproneVersion")
    errorprone("com.uber.nullaway:nullaway:$nullawayVersion")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
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

        eclipse().configFile(rootProject.file("config/eclipse-formatter.properties"))
        removeUnusedImports()
        forbidWildcardImports()
        importOrder("", "javax|java", "\\#")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
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
