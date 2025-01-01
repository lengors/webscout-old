package io.github.lengors.webscout

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import reactor.core.publisher.Hooks

@EnableCaching
@SpringBootApplication
@ConfigurationPropertiesScan
class WebscoutApplication {
    @PostConstruct
    fun setup() = Hooks.enableAutomaticContextPropagation()
}

fun main(args: Array<String>) {
    runApplication<WebscoutApplication>(*args)
}
