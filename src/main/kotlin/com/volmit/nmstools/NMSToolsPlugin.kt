package com.volmit.nmstools

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files

class NMSToolsPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        plugins.apply(JavaPlugin::class.java)

        val ext = extensions.create("nmsTools", NMSToolsExtension::class.java, project)
        val java = extensions.findByType(JavaPluginExtension::class.java) ?: throw GradleException("Java plugin not found")
        java.toolchain.languageVersion.set(ext.jvm.map(JavaLanguageVersion::of))

        val remap = tasks.create("remap", RemapTask::class.java, ext)
        tasks.getByName("build").finalizedBy(remap)

        val repository: ArtifactRepository
        if (rootProject.useBuiltTools) {
            tasks.register("setup") {
                it.group = "nms"
                it.dependsOn("clean")
                it.doLast {
                    runBuildTools(project, ext, true)
                }
            }

            repository = repositories.mavenLocal { mvn ->
                mvn.name = "nms"
                mvn.content {
                    it.includeGroup("org.bukkit")
                    it.includeGroup("org.spigotmc")
                }
            }
        } else {
            repository = repositories.maven { mvn ->
                mvn.name = "nms"
                mvn.url = URI.create(rootProject.repositoryUrl)
                mvn.content {
                    it.includeGroup("org.bukkit")
                    it.includeGroup("org.spigotmc")
                }
            }
        }

        try {
            repositories.addFirst(repository)
        } catch (_: Throwable) {}

        afterEvaluate {
            val version = ext.version.get()
            if (rootProject.useBuiltTools)
                runBuildTools(project, ext, false)

            configurations.create("maps")
            configurations.create("jars")

            dependencies.add("compileOnly", "org.spigotmc:spigot-api:$version")
            dependencies.add("compileOnly", "org.spigotmc:spigot:$version:remapped-mojang")

            dependencies.add("maps", "org.spigotmc:minecraft-server:$version:maps-spigot@csrg")
            dependencies.add("maps", "org.spigotmc:minecraft-server:$version:maps-spigot-members@csrg")
            dependencies.add("maps", "org.spigotmc:minecraft-server:$version:maps-mojang@txt")

            dependencies.add("jars", "org.spigotmc:spigot:$version:remapped-mojang")
            dependencies.add("jars", "org.spigotmc:spigot:$version:remapped-obf")
        }
    }

    private fun runBuildTools(project: Project, extension: NMSToolsExtension, force: Boolean): Unit = with(project) {
        val java = extensions.findByType(JavaPluginExtension::class.java) ?: throw GradleException("Java plugin not found")

        val jvm = extension.jvm.get()
        val version = extension.version.get()

        val buildToolsJar = rootProject.layout.buildDirectory.asFile.get().resolve("tools/BuildTools.jar")
        val m2 = File(System.getProperty("user.home"), ".m2/repository")

        val buildDir = layout.buildDirectory.asFile.get()
        val buildToolsFolder = buildDir.resolve("buildtools")
        val buildToolsHint = File(m2, "org/bukkit/craftbukkit/$version/craftbukkit-$version-remapped-mojang.jar")
        val executable = extension.executable.get()

        if (!force) {
            val javaVersion = JavaVersion.toVersion(jvm)
            java.sourceCompatibility = javaVersion
            java.targetCompatibility = javaVersion

            if (buildToolsHint.exists()) return
        }

        if (!buildToolsJar.exists()) {
            if (!buildToolsJar.parentFile.exists() && !buildToolsJar.parentFile.mkdirs())
                throw GradleException("BuildTools jar could not be created")
            URL("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar")
                .openStream().use { Files.copy(it, buildToolsJar.toPath()) }
        }

        if (!buildToolsFolder.exists() && !buildToolsFolder.mkdirs())
            throw GradleException("BuildTools folder could not be created")

        javaexec {
            it.executable = executable.absolutePath
            it.classpath = files(buildToolsJar)
            it.workingDir = buildToolsFolder
            it.args = listOf(
                "--rev",
                version.split("-")[0],
                "--compile",
                "craftbukkit",
                "--remap")
            val env = HashMap(it.environment)
            env["JAVA_HOME"] = executable.parentFile.parentFile.absolutePath
            it.environment = env
        }
    }
}