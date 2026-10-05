pluginManagement {
    val kotlin_version = providers.gradleProperty("kotlin_version").get()
    val spotless_version = providers.gradleProperty("spotless_version").get()
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("com.diffplug.spotless") version spotless_version
    }
}


plugins {
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

val centralPortalUsername = providers.gradleProperty("centralPortalUsername")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_USERNAME")).getOrElse("")
val centralPortalPassword = providers.gradleProperty("centralPortalPassword")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_PASSWORD")).getOrElse("")

nmcpSettings {
    centralPortal {
        username = centralPortalUsername
        password = centralPortalPassword
        publishingType = "USER_MANAGED"
        publicationName = "top.skyeyefast:taiwan-mahjong:${providers.gradleProperty("version").get()}"
    }
}

rootProject.name = "taiwan-mahjong"
