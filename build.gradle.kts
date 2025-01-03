plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.spring.dependency-management")
}

group = "io.github.lengors"

val commonsValidatorVersion: String by properties
val protoscoutVersion: String by properties
val hazelcastVersion: String by properties
val caffeineVersion: String by properties
val openapiVersion: String by properties
val ktlintVersion: String by properties
val monetaVersion: String by properties
val jsoupVersion: String by properties
val jexlVersion: String by properties
val javaVersion: String by properties

java.toolchain {
    languageVersion = JavaLanguageVersion.of(javaVersion)
}

configurations.compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.micrometer:context-propagation")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework:spring-jdbc")
    implementation("io.github.lengors:protoscout:$protoscoutVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("com.hazelcast:hazelcast-spring:$hazelcastVersion")
    implementation("org.apache.commons:commons-jexl3:$jexlVersion")
    implementation("commons-validator:commons-validator:$commonsValidatorVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("org.javamoney:moneta:$monetaVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$openapiVersion")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin.compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
}

ktlint {
    version = ktlintVersion
}

tasks {
    bootBuildImage {
        tags = listOf("webscout")

        docker {
            publishRegistry {
                System
                    .getenv("DOKCER_PUBLISH_REGISTRY_URL")
                    ?.let(url::set)
                System
                    .getenv("DOKCER_PUBLISH_REGISTRY_USERNAME")
                    ?.let(username::set)
                System
                    .getenv("DOKCER_PUBLISH_REGISTRY_PASSWORD")
                    ?.let(password::set)
                System
                    .getenv("DOKCER_PUBLISH_REGISTRY_TOKEN")
                    ?.let(token::set)
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
