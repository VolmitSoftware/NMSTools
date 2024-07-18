package com.volmit.nmstools

import org.gradle.api.Project


val Project.repositoryUrl: String
    get() = findProperty("nmsTools.repo-url")?.toString() ?: "https://repo.codemc.org/repository/nms/"

val Project.useBuiltTools: Boolean
    get() = findProperty("nmsTools.useBuildTools")?.toString()?.toBoolean() ?: false

val Project.specialSourceVersion: String
    get() = findProperty("nmsTools.specialSourceVersion")?.toString() ?: "1.11.4"