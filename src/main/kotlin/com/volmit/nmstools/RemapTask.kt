package com.volmit.nmstools

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.net.URL
import java.nio.file.Files
import javax.inject.Inject

@CacheableTask
abstract class RemapTask @Inject constructor(
    @Transient private val extension: NMSToolsExtension
) : DefaultTask() {
    private val specialSourceJar = project.rootProject.layout.buildDirectory.asFile.get().resolve("tools/SpecialSource-${project.specialSourceVersion}.jar")

    @InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputFile: File = getInputFile(project)
    private val tmpDirectory: File = project.layout.buildDirectory.asFile.get().resolve("specialsource")

    @OutputFile
    val outputFile: File = getOutputFile(inputFile)

    init {
        group = "nms"
        dependsOn("jar")
    }

    @TaskAction
    internal fun run() {
        if (!tmpDirectory.exists() && !tmpDirectory.mkdirs())
            throw GradleException("Create directory ${tmpDirectory.absolutePath} could not be created")

        downloadJar()
        obfuscate()
        remap()
        remapMembers()
    }

    private fun downloadJar(): Unit = with(project) {
        if (specialSourceJar.exists()) return

        if (!specialSourceJar.parentFile.exists() && !specialSourceJar.parentFile.mkdirs())
            throw GradleException("SpecialSource jar could not be created")
        URL("https://repo.maven.apache.org/maven2/net/md-5/SpecialSource/${specialSourceVersion}/SpecialSource-${specialSourceVersion}-shaded.jar")
            .openStream().use { Files.copy(it, specialSourceJar.toPath()) }
    }


    private fun obfuscate(): Unit = with(project) {
        project.javaexec {
            it.executable = getExecutable().absolutePath
            it.workingDir = tmpDirectory
            it.classpath = files(specialSourceJar, findJar("remapped-mojang.jar"))
            it.mainClass.set("net.md_5.specialsource.SpecialSource")
            it.args = listOf(
                "--live",
                "-i",
                inputFile.absolutePath,
                "-o",
                "obfuscated.jar",
                "-m",
                findMap("maps-mojang.txt"),
                "--reverse")
            val env = HashMap(it.environment)
            env["JAVA_HOME"] = getExecutable().parentFile.parentFile.absolutePath
            it.environment = env
        }
    }

    private fun remap(): Unit = with(project) {
        project.javaexec {
            it.executable = getExecutable().absolutePath
            it.workingDir = tmpDirectory
            it.classpath = files(specialSourceJar, findJar("remapped-obf.jar"))
            it.mainClass.set("net.md_5.specialsource.SpecialSource")
            it.args = listOf(
                "--live",
                "-i",
                "obfuscated.jar",
                "-o",
                "remapped.jar",
                "-m",
                findMap("maps-spigot.csrg"))
            val env = HashMap(it.environment)
            env["JAVA_HOME"] = getExecutable().parentFile.parentFile.absolutePath
            it.environment = env
        }
    }

    private fun remapMembers(): Unit = with(project) {
        project.javaexec {
            it.executable = getExecutable().absolutePath
            it.workingDir = tmpDirectory
            it.classpath = files(specialSourceJar, findJar("remapped-obf.jar"))
            it.mainClass.set("net.md_5.specialsource.SpecialSource")
            it.args = listOf(
                "--live",
                "-i",
                "remapped.jar",
                "-o",
                outputFile.absolutePath,
                "-m",
                findMap("maps-spigot-members.csrg"))
            val env = HashMap(it.environment)
            env["JAVA_HOME"] = getExecutable().parentFile.parentFile.absolutePath
            it.environment = env
        }
    }

    private fun findJar(name: String): File {
        return find("jars", name)
    }

    private fun findMap(name: String): String {
        return find("maps", name).absolutePath
    }

    private fun find(config: String, name: String): File {
        return project.configurations.getByName(config)
            .resolve()
            .stream()
            .filter{ it.name.endsWith(name) }
            .findFirst()
            .get()
    }

    private fun getExecutable(): File {
        return extension.executable.get()
    }
}

private fun getInputFile(project: Project): File {
    val task: Jar = project.tasks.getByName("jar") as Jar
    return task.archiveFile.get().asFile
}

private fun getOutputFile(inputFile: File): File {
    return File(inputFile.parentFile, "${inputFile.nameWithoutExtension}-mapped.jar")
}