pluginManagement {
    val springDependencyManagementVersion: String by settings
    val ktlintPluginVersion: String by settings
    val springBootVersion: String by settings
    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        id("org.springframework.boot") version springBootVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintPluginVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
    }
}

rootProject.name = "webscout"
