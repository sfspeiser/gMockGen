import org.gradle.internal.jvm.Jvm
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    kotlin("jvm") version "1.9.22"
    java
    idea
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

val gradleToolingExtension: Configuration by configurations.creating
val testLibs: Configuration by configurations.creating {
    isTransitive = false
}

group = "sspeiser"
version = "0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }
}
kotlin {
    jvmToolchain {
        languageVersion.set(java.toolchain.languageVersion.get())
    }
}

val gradleToolingExtensionSourceSet: SourceSet = sourceSets.create("gradle-tooling-extension") {
    configurations.named(compileOnlyConfigurationName) {
        extendsFrom(gradleToolingExtension)
    }
}
val gradleToolingExtensionJar = tasks.register<Jar>(gradleToolingExtensionSourceSet.jarTaskName) {
    from(gradleToolingExtensionSourceSet.output)
    archiveClassifier.set("gradle-tooling-extension")
    exclude("META-INF/plugin.xml")
}

val externalAnnotationsJar = tasks.register<Jar>("externalAnnotationsJar") {
    from("externalAnnotations")
    destinationDirectory.set(layout.buildDirectory.dir("externalAnnotations"))
    archiveFileName.set("externalAnnotations.jar")
}

repositories {
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Add tools.jar for the JDI API
    implementation(files(Jvm.current().toolsJar))
    implementation("joda-time:joda-time:2.12.7")
    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(files(gradleToolingExtensionJar))
    intellijPlatform {
        clion("2024.1")
        bundledPlugin("com.intellij.clion")
        bundledPlugin("com.intellij.cidr.base")
        bundledPlugin("com.intellij.cidr.lang")

        bundledPlugin("org.intellij.intelliLang")
        instrumentationTools()
    }
}
intellijPlatform {
    signing {
//        if (System.getenv("SIGNER_CLI") != null && System.getenv("PRIVATEKEY") != null && System.getenv("PRIVATEKEY") != null) {
            cliPath = file(System.getenv("SIGNER_CLI"))
            privateKeyFile = file(System.getenv("PRIVATEKEY"))
            certificateChainFile = file(System.getenv("CERT"))
//        }
    }
    pluginVerification {
        cliPath = file(System.getenv("VERIFIER_CLI"))
        failureLevel = VerifyPluginTask.FailureLevel.ALL
        verificationReportsFormats = VerifyPluginTask.VerificationReportsFormats.ALL
        teamCityOutputFormat = false
        subsystemsToCheck = VerifyPluginTask.Subsystems.ALL

        ides {
            recommended()
            select {
                types = listOf(IntelliJPlatformType.CLion)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = "232"
                untilBuild = "242.*"
            }
        }
    }
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}


idea {
    module {
        generatedSourceDirs.add(file("build/gen"))
        excludeDirs.add(intellijPlatform.sandboxContainer.get().asFile)
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}