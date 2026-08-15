plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

val minecraftVersion: String = project.property("minecraftVersion") as String
val javaVersion: String = project.property("javaVersion") as String
val apiVersion = minecraftVersion.split('.').take(2).joinToString(".")

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
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
