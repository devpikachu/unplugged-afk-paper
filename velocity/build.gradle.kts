import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("com.gradleup.shadow")
    id("net.ltgt.errorprone")
}

val velocityVersion = libs.versions.velocity.asProvider().get()

fun ShadowJar.sharedShading() {
    exclude(
        "META-INF/INDEX.LIST",
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "module-info.class",
        "META-INF/versions/**/module-info.class"
    )

    relocate("de.exlll.configlib", "dev.detpikachu.unpluggedafk.libs.configlib")
    relocate("org.snakeyaml.engine", "dev.detpikachu.unpluggedafk.libs.snakeyaml.engine")

    minimize()
}

data class Proxy(
    val velocity: String,
    val release: Int,
    val api: Provider<MinimalExternalModuleDependency>
)

val extraProxies = listOf(
    Proxy(libs.versions.velocity.v400.get(), 25, libs.velocity.api.v400),
)

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

base {
    archivesName = "unplugged-afk-velocity-$velocityVersion"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
}

val proxyShared = configurations.dependencyScope("proxyShared").get()

configurations.compileOnly {
    extendsFrom(proxyShared)
}

dependencies {
    // Velocity
    compileOnly(libs.velocity.api.asProvider())
    annotationProcessor(libs.velocity.api.asProvider())

    // Common
    implementation(project(":common"))

    // Integrations
    proxyShared(libs.miniplaceholders.api)

    // Dependencies
    proxyShared(libs.netty.buffer)
    proxyShared(libs.netty.codec.base)
    proxyShared(libs.netty.handler)
    proxyShared(libs.netty.transport)

    // Annotations
    proxyShared(libs.jspecify)
    proxyShared(libs.jetbrains.annotations)

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

        sharedShading()
    }

    compileJava {
        options.release = 21
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

extraProxies.forEach { proxy ->
    val slug = proxy.velocity.replace('.', '_')

    val proxyCompileClasspath = configurations.resolvable("proxy${slug}CompileClasspath") {
        extendsFrom(proxyShared, configurations.implementation.get())
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_API))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named<LibraryElements>(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named<Bundling>(Bundling.EXTERNAL))
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, proxy.release)
        }
    }

    dependencies.add(proxyCompileClasspath.name, proxy.api)

    val proxyAnnotationProcessor = configurations.resolvable("proxy${slug}AnnotationProcessor") {
        extendsFrom(configurations.getByName("errorprone"))
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named<LibraryElements>(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named<Bundling>(Bundling.EXTERNAL))
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, proxy.release)
        }
    }

    dependencies.add(proxyAnnotationProcessor.name, proxy.api)

    val compileProxy = tasks.register<JavaCompile>("compileProxy${slug}Java") {
        source(sourceSets.main.get().java)
        classpath = files(proxyCompileClasspath)
        destinationDirectory = layout.buildDirectory.dir("classes/java/proxy$slug")
        options.release = proxy.release

        options.annotationProcessorPath = files(proxyAnnotationProcessor)
        options.errorprone {
            enabled = true
        }
    }

    val shadowProxy = tasks.register<ShadowJar>("shadowProxy${slug}Jar") {
        archiveBaseName = "unplugged-afk-velocity-${proxy.velocity}"
        archiveClassifier = ""

        from(compileProxy.flatMap { it.destinationDirectory })
        configurations = listOf(project.configurations.runtimeClasspath.get())

        sharedShading()
    }

    tasks.assemble {
        dependsOn(shadowProxy)
    }
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
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
    options.errorprone {
        disableWarningsInGeneratedCode = true

        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
    }
}
