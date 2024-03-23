import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.9.23"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
    kotlin("plugin.serialization") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.openjfx.javafxplugin") version "0.1.0"

}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // This dependency is used by the application.
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.kotlinmath:complex-numbers:1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.github.xn32:json5k:0.3.0")
    implementation("eu.mihosoft.vrl.jcsg:jcsg:0.5.7")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("com.github.ajalt.mordant:mordant:2.4.0")

}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("1.9.23")
        }
    }
}

javafx {
    modules("javafx.controls", "javafx.fxml")
}

application {
    // Define the main class for the application.
    mainClass.set("org.goodmath.chalumier.cli.ChalumierKt")
    applicationName = "chalumier"
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("chalumier")
        archiveClassifier.set("")
        archiveVersion.set("0.0.1")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "org.goodmath.chalumier.cli.ChalumierKt"))
        }
    }
}

