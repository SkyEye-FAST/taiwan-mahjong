pluginManagement {
    val kotlin_version = providers.gradleProperty("kotlin_version").get()
    val spotless_version = providers.gradleProperty("spotless_version").get()
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("com.diffplug.spotless") version spotless_version
    }
}

rootProject.name = "taiwan-mahjong"
