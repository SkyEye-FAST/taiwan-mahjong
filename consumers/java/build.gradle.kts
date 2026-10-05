plugins { application }

repositories {
    mavenLocal {
        content { includeModule("top.skyeyefast", "taiwan-mahjong") }
        // Exercise the Maven POM, not Gradle's .module file or a project dependency.
        metadataSources {
            mavenPom()
            ignoreGradleMetadataRedirection()
        }
    }
    mavenCentral { content { excludeModule("top.skyeyefast", "taiwan-mahjong") } }
}

dependencies { implementation("top.skyeyefast:taiwan-mahjong:0.1.0") }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
application { mainClass.set("example.JavaConsumer") }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
