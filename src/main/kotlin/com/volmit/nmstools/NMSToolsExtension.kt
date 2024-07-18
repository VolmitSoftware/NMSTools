package com.volmit.nmstools

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.internal.DefaultToolchainSpec
import java.io.File

abstract class NMSToolsExtension(private val project: Project) {
    val jvm: Property<Int> = project.objects.property(Int::class.java)
    val version: Property<String> = project.objects.property(String::class.java)

    val executable: Provider<File> = jvm.map {
        val javaToolchains = project.extensions.getByType(JavaToolchainService::class.java) ?: throw GradleException("Java toolchain service not found")
        val spec = DefaultToolchainSpec(project.objects)
        spec.languageVersion.set(JavaLanguageVersion.of(it))

        return@map javaToolchains.launcherFor(spec).get().executablePath.asFile
    }
}