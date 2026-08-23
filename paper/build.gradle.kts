import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("com.gradleup.shadow")
    id("io.papermc.paperweight.userdev")
    id("net.ltgt.errorprone")
    id("xyz.jpenilla.run-paper")
}

val minecraftVersion = libs.versions.minecraft.get()
val apiVersion = minecraftVersion.split('.').take(2).joinToString(".")

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

repositories {
    maven("https://repo.william278.net/releases")
    maven("https://repo.extendedclip.com/releases")
}

base {
    archivesName = "unplugged-afk"
}

dependencies {
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

    implementation(project(":common"))

    compileOnly(libs.husksync.bukkit)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.errorprone.annotations)
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

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier = ""
    }

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
