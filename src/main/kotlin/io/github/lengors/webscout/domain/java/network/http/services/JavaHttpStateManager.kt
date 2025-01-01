package io.github.lengors.webscout.domain.java.network.http.services

import io.github.lengors.webscout.domain.network.http.services.HttpStateManager
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.CookieManager
import java.net.HttpCookie
import java.net.URI

@JvmInline
value class JavaHttpStateManager(
    private val cookieManager: CookieManager = CookieManager(),
) : HttpStateManager {
    companion object {
        private val logger = LoggerFactory.getLogger(JavaHttpStateManager::class.java)
    }

    override fun get(uri: URI): List<HttpCookie> = cookieManager.cookieStore.get(uri)

    override fun set(
        uri: URI,
        headers: Map<String, List<String>>,
    ) {
        try {
            cookieManager.put(uri, headers)
        } catch (exception: IOException) {
            logger.error("Failed to store state", exception)
        }
    }
}
