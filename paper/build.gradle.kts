import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("com.gradleup.shadow")
    id("io.papermc.paperweight.userdev")
    id("net.ltgt.errorprone")
    id("xyz.jpenilla.run-paper")
}

fun apiVersionFor(minecraft: String): String =
    if (minecraft.startsWith("1.")) minecraft.split('.').take(2).joinToString(".") else minecraft

val minecraftVersion = libs.versions.minecraft.get()
val apiVersion = apiVersionFor(minecraftVersion)

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

data class Backend(
    val minecraft: String,
    val release: Int,
    val husksync: Provider<MinimalExternalModuleDependency>,
    val symbols: Set<String>
)

val extraBackends = listOf(
    Backend("26.1.2", 25, libs.husksync.bukkit.v2612, setOf("MC_26")),
    Backend("26.2", 25, libs.husksync.bukkit.v262, setOf("MC_26", "MC_26_2")),
)

abstract class PreprocessSources : DefaultTask() {

    @get:InputDirectory
    abstract val source: DirectoryProperty

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @get:Input
    abstract val symbols: SetProperty<String>

    @TaskAction
    fun preprocess() {
        val defined = symbols.get()
        val from = source.get().asFile
        val into = destination.get().asFile

        into.deleteRecursively()

        from.walkTopDown().filter { it.isFile }.forEach { file ->
            val target = into.resolve(file.toRelativeString(from))
            target.parentFile.mkdirs()

            if (file.extension == "java") {
                target.writeText(rewrite(file.readLines(), defined).joinToString("\n", postfix = "\n"))
            } else {
                file.copyTo(target, overwrite = true)
            }
        }
    }

    private fun rewrite(lines: List<String>, defined: Set<String>): List<String> {
        val branch = ArrayDeque<Boolean>()

        val rewritten = lines.map { line ->
            val directive = directiveOf(line)

            when {
                directive != null && directive.startsWith(IF) -> {
                    branch.addLast(evaluate(directive.removePrefix(IF), defined))
                    line
                }
                directive == ELSE -> {
                    branch.addLast(!branch.removeLast())
                    line
                }
                directive == ENDIF -> {
                    branch.removeLast()
                    line
                }
                branch.isEmpty() -> line
                branch.all { it } -> uncomment(line)
                else -> comment(line)
            }
        }

        require(branch.isEmpty()) { "unbalanced //$IF in ${source.get().asFile}" }

        return rewritten
    }

    private fun directiveOf(line: String): String? {
        val token = line.trim()

        return if (token.startsWith("//")) token.removePrefix("//").trimStart() else null
    }

    private fun evaluate(expression: String, defined: Set<String>): Boolean {
        val token = expression.trim()

        return if (token.startsWith("!")) token.removePrefix("!").trim() !in defined else token in defined
    }

    private fun uncomment(line: String): String {
        val directive = directiveOf(line)

        if (directive == null || !directive.startsWith(HIDDEN)) {
            return line
        }

        return line.takeWhile { it == ' ' } + directive.removePrefix(HIDDEN).removePrefix(" ")
    }

    private fun comment(line: String): String {
        if (line.isBlank() || directiveOf(line)?.startsWith(HIDDEN) == true) {
            return line
        }

        val indent = line.takeWhile { it == ' ' }

        return indent + "// " + HIDDEN + " " + line.substring(indent.length)
    }

    private companion object {
        const val IF = "#if "
        const val ELSE = "#else"
        const val ENDIF = "#endif"
        const val HIDDEN = "\$\$"
    }
}

repositories {
    maven("https://repo.william278.net/releases") {
        content { includeGroupByRegex("net\\.william278.*") }
    }
    maven("https://repo.extendedclip.com/releases") {
        content { includeGroup("me.clip") }
    }
}

base {
    archivesName = "unplugged-afk-$minecraftVersion"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

val backendShared = configurations.dependencyScope("backendShared").get()

configurations.compileOnly {
    extendsFrom(backendShared)
}

dependencies {
    // Paper
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

    // Common
    implementation(project(":common"))

    // Dependencies
    backendShared(libs.netty.buffer)
    backendShared(libs.netty.codec.base)
    backendShared(libs.netty.handler)
    backendShared(libs.netty.transport)

    // Integrations
    compileOnly(libs.husksync.bukkit.asProvider())
    backendShared(libs.placeholderapi)
    backendShared(libs.luckperms.api)
    backendShared(libs.miniplaceholders.api)

    // Annotations
    backendShared(libs.errorprone.annotations)
    backendShared(libs.jspecify)
    backendShared(libs.jetbrains.annotations)

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
        inputs.properties(props)
        filesMatching(listOf("plugin.yml", "unplugged-afk.properties")) {
            expand(props)
        }
    }

    compileJava {
        options.release = 21
    }
}

extraBackends.forEach { backend ->
    val slug = backend.minecraft.replace('.', '_')

    val backendServer = configurations.dependencyScope("backend${slug}Server")

    val backendCompileClasspath = configurations.resolvable("backend${slug}CompileClasspath") {
        extendsFrom(backendShared, configurations.implementation.get(), backendServer.get())
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_API))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(Category.LIBRARY))
            attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named<LibraryElements>(LibraryElements.CLASSES)
            )
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named<Bundling>(Bundling.EXTERNAL))
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, backend.release)
        }
    }

    dependencies {
        add(backendServer.name, project(":paper-${backend.minecraft}"))
        add(backendServer.name, backend.husksync)
    }

    val preprocessBackend = tasks.register<PreprocessSources>("preprocessBackend${slug}Sources") {
        source = layout.projectDirectory.dir("src/main/java")
        destination = layout.buildDirectory.dir("generated/preprocessed/backend$slug")
        symbols = backend.symbols
    }

    val compileBackend = tasks.register<JavaCompile>("compileBackend${slug}Java") {
        source(preprocessBackend.flatMap { it.destination })
        classpath = files(backendCompileClasspath)
        destinationDirectory = layout.buildDirectory.dir("classes/java/backend$slug")
        options.release = backend.release

        options.annotationProcessorPath = sourceSets.main.get().annotationProcessorPath
        options.errorprone {
            enabled = true
        }
    }

    val processBackendResources = tasks.register<ProcessResources>("processBackend${slug}Resources") {
        val props = mapOf(
            "version" to version,
            "apiVersion" to apiVersionFor(backend.minecraft),
            "minecraftVersion" to backend.minecraft
        )
        inputs.properties(props)
        from(sourceSets.main.get().resources)
        into(layout.buildDirectory.dir("resources/backend$slug"))
        filesMatching(listOf("plugin.yml", "unplugged-afk.properties")) {
            expand(props)
        }
    }

    val shadowBackend = tasks.register<ShadowJar>("shadowBackend${slug}Jar") {
        archiveBaseName = "unplugged-afk-${backend.minecraft}"
        archiveClassifier = ""

        from(compileBackend.flatMap { it.destinationDirectory })
        from(processBackendResources)
        configurations = listOf(project.configurations.runtimeClasspath.get())

        sharedShading()
    }

    tasks.assemble {
        dependsOn(shadowBackend)
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
    options.compilerArgs.addAll(listOf("-Xlint:all,-classfile,-serial", "-Werror"))
    options.errorprone {
        disableWarningsInGeneratedCode = true

        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
    }
}
