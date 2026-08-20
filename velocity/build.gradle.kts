plugins {
    id("java-library")
}

val velocityVersion: String = project.property("velocityVersion") as String
val javaVersion: String = project.property("javaVersion") as String

base {
    archivesName = "unplugged-afk-velocity"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
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
