package com.volmit.nmstools

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import java.io.File

abstract class NMSToolsExtension(private val project: Project) {
    val jvm: Property<Int> = project.objects.property(Int::class.java)
    val version: Property<String> = project.objects.property(String::class.java)

    val executable: Provider<File> = jvm.flatMap { version ->
        val javaToolchains = project.extensions.getByType(JavaToolchainService::class.java) ?: throw GradleException("Java toolchain service not found")
        javaToolchains.launcherFor { it.languageVersion.set(JavaLanguageVersion.of(version)) }
            .map { it.executablePath.asFile }
    }
}