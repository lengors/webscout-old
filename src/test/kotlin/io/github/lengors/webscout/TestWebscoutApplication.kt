package io.github.lengors.webscout

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<WebscoutApplication>().with(TestcontainersConfiguration::class).run(*args)
}
