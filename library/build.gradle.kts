import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
}

group = "io.github.littlesurvival"
version = "1.1.27"

kotlin {
    jvm()
    androidLibrary {
        namespace = "io.github.littlesurvival"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }

        compilations.configureEach { compilerOptions.configure { jvmTarget.set(JvmTarget.JVM_11) } }
    }
    iosArm64()
    iosSimulatorArm64()
    // linuxX64 removed — Compose Multiplatform does not support Linux native targets.
    // Pure-Kotlin code (AST, parser, config) still compiles for all targets via commonMain.

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ksoup)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }

        commonTest.dependencies { implementation(libs.kotlin.test) }

        jvmTest.dependencies { implementation(libs.ktor.client.mock) }

        androidMain.dependencies { implementation(libs.ktor.client.android) }

        iosMain.dependencies { implementation(libs.ktor.client.darwin) }

        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (!project.providers.gradleProperty("skipSigning").isPresent) {
        signAllPublications()
    }

    coordinates(group.toString(), "yamibo-api", version.toString())

    pom {
        name = "yamibo-api"
        description = "Yamibo API library."
        inceptionYear = "2026"
        url = "https://github.com/LittleSurvival/yamibo-api"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "littlesurvival"
                name = "littlesurvival"
                url = "https://github.com/LittleSurvival"
            }
        }
        scm {
            url = "https://github.com/LittleSurvival/yamibo-api"
            connection = "scm:git:https://github.com/LittleSurvival/yamibo-api.git"
            developerConnection = "scm:git:ssh://git@github.com/LittleSurvival/yamibo-api.git"
        }
    }
}
