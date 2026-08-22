import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("io.papermc.paperweight.userdev")
    id("net.ltgt.errorprone")
    id("xyz.jpenilla.run-paper")
}

val minecraftVersion: String = project.property("minecraftVersion") as String
val huskSyncVersion: String = project.property("huskSyncVersion") as String

val javaVersion: String = project.property("javaVersion") as String

val errorproneVersion: String = project.property("errorproneVersion") as String
val nullawayVersion: String = project.property("nullawayVersion") as String
val jspecifyVersion: String = project.property("jspecifyVersion") as String
val jetbrainsAnnotationsVersion: String = project.property("jetbrainsAnnotationsVersion") as String

val apiVersion = minecraftVersion.split('.').take(2).joinToString(".")

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

repositories {
    maven("https://repo.william278.net/releases")
}

base {
    archivesName = "unplugged-afk"
}

dependencies {
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

    compileOnly("net.william278.husksync:husksync-bukkit:$huskSyncVersion")
    compileOnly("com.google.errorprone:error_prone_annotations:$errorproneVersion")
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

tasks {
    runServer {
        minecraftVersion(minecraftVersion)
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf(
            "version" to version,
            "apiVersion" to apiVersion,
            "minecraftVersion" to minecraftVersion
        )
        filesMatching(listOf("plugin.yml", "unplugged-afk.properties")) {
            expand(props)
        }
    }
}
