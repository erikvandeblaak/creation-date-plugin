import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "nl.vandeblaak"
version = providers.gradleProperty("pluginVersion").get()

val repoUrl = "https://github.com/erikvandeblaak/creation-date-plugin"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val localIde = providers.gradleProperty("phpstormPath")
        if (localIde.isPresent) {
            local(localIde)
        } else {
            phpstorm(providers.gradleProperty("platformVersion"))
        }
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

/** The bullet list of this version in CHANGELOG.md, as HTML for the plugin manager. */
fun changeNotes(version: String): String {
    val lines = file("CHANGELOG.md").readLines()
    val start = lines.indexOfFirst { it.startsWith("## [$version]") }
    require(start >= 0) { "CHANGELOG.md has no section for $version" }
    val items = lines.drop(start + 1)
        .takeWhile { !it.startsWith("## ") }
        .filter { it.startsWith("- ") }
        .map { "<li>" + it.removePrefix("- ").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</li>" }
    require(items.isNotEmpty()) { "CHANGELOG.md section $version has no items" }
    return "<ul>${items.joinToString("")}</ul>"
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        version = project.version.toString()
        changeNotes = providers.provider { changeNotes(project.version.toString()) }
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // No upper limit: keep working on new PhpStorm versions
            untilBuild = providers.provider { null }
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.PhpStorm, providers.gradleProperty("platformVersion"))
            create(IntelliJPlatformType.PhpStorm, providers.gradleProperty("verifyPlatformVersion"))
        }
    }
}

tasks.verifyPlugin {
    // Pass a custom trust store (e.g. for a TLS-inspecting virus scanner) on to the verifier process
    listOf("javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword").forEach { key ->
        System.getProperty(key)?.let { jvmArgs("-D$key=$it") }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

/**
 * Writes updatePlugins.xml: the custom plugin repository that PhpStorm reads to find updates.
 * The release workflow attaches it to every GitHub release, so
 * <repoUrl>/releases/latest/download/updatePlugins.xml always describes the newest version.
 */
val updatePluginsXml by tasks.registering {
    val pluginVersion = project.version.toString()
    val sinceBuild = providers.gradleProperty("pluginSinceBuild")
    val notes = providers.provider { changeNotes(pluginVersion) }
    val zipName = "creation-date-plugin-$pluginVersion.zip"
    val output = layout.buildDirectory.file("distributions/updatePlugins.xml")
    outputs.file(output)
    inputs.property("version", pluginVersion)
    inputs.property("notes", notes)
    doLast {
        output.get().asFile.writeText(
            """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<plugins>
            |    <plugin id="nl.vandeblaak.creationdate" version="$pluginVersion"
            |            url="$repoUrl/releases/download/v$pluginVersion/$zipName">
            |        <name>Creation Date in Project View</name>
            |        <vendor url="https://www.vandeblaak.com/">Erik van de Blaak</vendor>
            |        <idea-version since-build="${sinceBuild.get()}"/>
            |        <description>Shows the creation date of files and folders in a balloon when you hover over them in the Project tool window.</description>
            |        <change-notes><![CDATA[${notes.get()}]]></change-notes>
            |    </plugin>
            |</plugins>
            |""".trimMargin()
        )
    }
}
