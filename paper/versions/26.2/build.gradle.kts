plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
}

paperweight {
    addServerDependencyTo = setOf(configurations.api.get())
}

dependencies {
    // Paper
    paperweight.paperDevBundle(libs.versions.paper.v262.get())
}
