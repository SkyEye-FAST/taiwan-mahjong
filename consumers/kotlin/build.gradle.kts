import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    application
}

repositories {
    mavenLocal { content { includeModule("top.skyeyefast", "taiwan-mahjong") } }
    mavenCentral { content { excludeModule("top.skyeyefast", "taiwan-mahjong") } }
}

dependencies { implementation("top.skyeyefast:taiwan-mahjong:0.1.0") }
kotlin.compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.add("-Xjdk-release=17")
}
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
application { mainClass.set("example.KotlinConsumerKt") }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
