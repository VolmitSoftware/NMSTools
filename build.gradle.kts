plugins {
    id("maven-publish")
    id("java-gradle-plugin")
    kotlin("jvm") version "2.0.0"
}

group = "com.volmit"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("NMSTools") {
            id = "com.volmit.nmstools"
            implementationClass = "com.volmit.nmstools.NMSToolsPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}