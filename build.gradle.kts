import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import groovy.json.JsonSlurper
import java.io.DataInputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    id("org.jetbrains.dokka") version "2.2.0"
    `java-library`
    `maven-publish`
    signing
}

group = "top.skyeyefast"
repositories { mavenCentral() }
kotlin {
    jvmToolchain(21)
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
        filters { include { byNames.add("top.skyeyefast.taiwan.**") } }
    }
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
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
dokka {
    dokkaPublications.html {
        offlineMode.set(true)
        failOnWarning.set(true)
    }
    dokkaSourceSets.main {
        jdkVersion.set(17)
    }
}

// Dokka API reference in the standard javadoc classifier.
tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    from(listOf("README.md", "RULES.md", "CHANGELOG.md", "LICENSE")) { into("guides") }
    from("consumers/README.md") { into("guides/consumers") }
}

// Inspect publication files with JDK/Gradle APIs, without adding library dependencies.
tasks.register("verifyPublication") {
    group = "verification"
    description = "Verify JVM target, license, source/docs JARs, POM and module metadata."
    val archives = objects.fileCollection().from(
        tasks.named("jar"), tasks.named("sourcesJar"), tasks.named("javadocJar"),
    )
    val pom = layout.buildDirectory.file("publications/maven/pom-default.xml")
    val metadata = layout.buildDirectory.file("publications/maven/module.json")
    val expectedVersion = project.version.toString()
    val centralPortalTaskConfigured = tasks.names.contains("publishAggregationToCentralPortal")
    val signingTaskConfigured = tasks.names.contains("signMavenPublication")
    inputs.files(archives, pom, metadata)
    inputs.property("artifactVersion", expectedVersion)
    dependsOn(archives, "generatePomFileForMavenPublication", "generateMetadataFileForMavenPublication")
    doLast {
        for (file in archives.files) ZipFile(file).use { zip ->
            fun text(path: String): String = zip.getInputStream(requireNotNull(zip.getEntry(path)) {
                "${file.name} is missing $path"
            }).bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(text("META-INF/licenses/taiwan-mahjong/LICENSE").contains("Apache License"))
            val entries = zip.entries().asSequence().filterNot { it.isDirectory }.toList()
            check(entries.none { it.name.endsWith(".cpp") || it.name.endsWith(".exe") || it.name.endsWith(".dll") })
            when {
                file.name.endsWith("-sources.jar") -> check(entries.any { it.name.endsWith("/TaiwanMahjong.kt") })
                file.name.endsWith("-javadoc.jar") -> {
                    check(text("index.html").contains("html", ignoreCase = true))
                    check(text("guides/RULES.md").isNotBlank())
                    check(entries.any { it.name.endsWith("/score.html") }) { "Missing generated score API documentation" }
                    check(entries.none { it.name.contains("/-distance/") || it.name.endsWith("/counts.html") || it.name.contains("/fixed-counts.html") })
                }
                else -> {
                    val classes = entries.filter { it.name.endsWith(".class") }
                    check(classes.isNotEmpty())
                    for (entry in classes) DataInputStream(zip.getInputStream(entry)).use { input ->
                        check(input.readInt() == 0xCAFEBABE.toInt())
                        input.readUnsignedShort()
                        check(input.readUnsignedShort() == 61) { "Not JVM 17: ${entry.name}" }
                    }
                }
            }
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val doc = factory.newDocumentBuilder().parse(pom.get().asFile)
        val xpath = XPathFactory.newInstance().newXPath()
        fun value(path: String): String = xpath.evaluate("string($path)", doc)
        check(value("/project/groupId") == "top.skyeyefast")
        check(value("/project/artifactId") == "taiwan-mahjong")
        check(value("/project/version") == expectedVersion)
        check(value("/project/url") == "https://github.com/SkyEye-FAST/taiwan-mahjong")
        check(value("/project/scm/connection") == "scm:git:https://github.com/SkyEye-FAST/taiwan-mahjong.git")
        check(value("/project/scm/developerConnection") == "scm:git:ssh://git@github.com/SkyEye-FAST/taiwan-mahjong.git")
        check(value("/project/scm/url") == "https://github.com/SkyEye-FAST/taiwan-mahjong")
        check(value("/project/licenses/license/name") == "Apache License 2.0")
        check(xpath.evaluate("count(/project/properties/*)", doc) == "0")
        check(value("/project/licenses/license/url") == "https://www.apache.org/licenses/LICENSE-2.0")
        check(value("/project/developers/developer/id") == "SkyEye-FAST")
        check(value("/project/dependencies/dependency/groupId") == "org.jetbrains.kotlin")
        check(value("/project/dependencyManagement/dependencies/dependency[groupId='org.jetbrains.kotlin' and artifactId='kotlin-stdlib']/version") == kotlin.coreLibrariesVersion)
        check(value("/project/dependencies/dependency/artifactId") == "kotlin-stdlib")
        check(value("/project/dependencies/dependency/scope") == "compile")
        check(xpath.evaluate("count(/project/dependencies/dependency)", doc) == "1")
        check(centralPortalTaskConfigured) { "Central Portal publication task is not configured" }
        check(signingTaskConfigured) { "Maven publication signing task is not configured" }
        val module = JsonSlurper().parse(metadata.get().asFile) as Map<*, *>
        check((module["component"] as Map<*, *>)["version"] == expectedVersion)
        val variants = (module["variants"] as List<*>).map { it as Map<*, *> }
        for (name in listOf("apiElements", "runtimeElements")) {
            val variant = variants.single { it["name"] == name }
            check((variant["attributes"] as Map<*, *>)["org.gradle.jvm.version"] == 17)
            val dependencies = (variant["dependencies"] as List<*>).map { it as Map<*, *> }
            check(dependencies.size == 1 && dependencies.single()["module"] == "kotlin-stdlib"
                && dependencies.single()["group"] == "org.jetbrains.kotlin")
        }
        logger.lifecycle("Verified Maven publication: JVM 17, API dependencies, sources, Dokka and Apache-2.0 license")
    }
}

tasks.check { dependsOn("verifyPublication") }

val centralSigningKey = providers.gradleProperty("signingKey")
    .orElse(providers.environmentVariable("MAVEN_CENTRAL_SIGNING_KEY"))
val centralSigningPassword = providers.gradleProperty("signingPassword")
    .orElse(providers.environmentVariable("MAVEN_CENTRAL_SIGNING_PASSWORD"))
val centralPortalUsername = providers.gradleProperty("centralPortalUsername")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_USERNAME"))
val centralPortalPassword = providers.gradleProperty("centralPortalPassword")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_PASSWORD"))

tasks.register("verifyCentralPublishingCredentials") {
    group = "verification"
    description = "Require Central Portal tokens and protected OpenPGP signing credentials before upload."
    val credentialsConfigured = listOf(
        centralPortalUsername.map { it.isNotBlank() }.getOrElse(false),
        centralPortalPassword.map { it.isNotBlank() }.getOrElse(false),
        centralSigningKey.map { it.isNotBlank() }.getOrElse(false),
        centralSigningPassword.map { it.isNotBlank() }.getOrElse(false),
    )
    doLast {
        check(credentialsConfigured[0]) { "Set CENTRAL_PORTAL_USERNAME or centralPortalUsername" }
        check(credentialsConfigured[1]) { "Set CENTRAL_PORTAL_PASSWORD or centralPortalPassword" }
        check(credentialsConfigured[2]) { "Set MAVEN_CENTRAL_SIGNING_KEY or signingKey" }
        check(credentialsConfigured[3]) { "Set MAVEN_CENTRAL_SIGNING_PASSWORD or signingPassword" }
    }
}

tasks.named("nmcpPublishAggregationToCentralPortal") {
    dependsOn("check", "verifyCentralPublishingCredentials")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "Taiwan Mahjong"
                description = "Taiwanese sixteen-tile hand structure, analysis and source-qualified tai scoring."
                url = "https://github.com/SkyEye-FAST/taiwan-mahjong"
                inceptionYear = "2026"
                scm {
                    connection = "scm:git:https://github.com/SkyEye-FAST/taiwan-mahjong.git"
                    developerConnection = "scm:git:ssh://git@github.com/SkyEye-FAST/taiwan-mahjong.git"
                    url = "https://github.com/SkyEye-FAST/taiwan-mahjong"
                }
                developers {
                    developer {
                        id = "SkyEye-FAST"
                        name = "SkyEye_FAST"
                        email = "skyeyefast@foxmail.com"
                        url = "https://github.com/SkyEye-FAST"
                    }
                }
                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                        distribution = "repo"
                    }
                }
            }
        }
    }
}

signing {
    centralSigningKey.orNull?.takeIf(String::isNotBlank)?.let { key ->
        useInMemoryPgpKeys(key, centralSigningPassword.orNull)
    }
    setRequired { gradle.taskGraph.hasTask("nmcpPublishAggregationToCentralPortal") }
    sign(publishing.publications["maven"])
}
