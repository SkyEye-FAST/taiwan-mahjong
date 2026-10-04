import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    `java-library`
    `maven-publish`
}

group = "top.skyeyefast"
repositories { mavenCentral() }
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
dependencies {
    api(kotlin("stdlib"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform("org.junit:junit-bom:${providers.gradleProperty("junit_version").get()}"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Four-meld remainder oracle only; never a runtime scoring authority.
    testImplementation("top.skyeyefast:mcr-mahjong:0.1.0")
}
tasks.test { useJUnitPlatform() }
spotless {
    kotlin {
        target("src/**/*.kt")
        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(4)
    }
    format("misc") {
        target("*.md", "*.gradle.kts", "*.properties", ".gitignore", ".gitattributes", ".editorconfig", ".github/workflows/*.yml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
tasks.withType<Jar>().configureEach {
    from("LICENSE") { into("META-INF/licenses/taiwan-mahjong") }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
publishing {
    publications.create<MavenPublication>("library") {
        from(components["java"])
        pom {
            name.set("taiwan-mahjong")
            description.set("Taiwanese sixteen-tile structure and source-qualified tai scoring")
            url.set("https://github.com/SkyEye-FAST/taiwan-mahjong")
            scm {
                url.set("https://github.com/SkyEye-FAST/taiwan-mahjong")
                connection.set("scm:git:https://github.com/SkyEye-FAST/taiwan-mahjong.git")
            }
            licenses { license { name.set("Apache License 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0") } }
        }
    }
}
