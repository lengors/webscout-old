package io.github.lengors.webscout.domain.channels

import kotlinx.coroutines.channels.SendChannel
import org.slf4j.Logger

suspend fun <T, U> SendChannel<U>.runCatching(
    logger: Logger,
    errorHandler: (Throwable) -> U,
    action: suspend () -> T,
): T? =
    try {
        action()
    } catch (throwable: Throwable) {
        errorHandler(throwable)
            .also { logger.error("Emitted response error: {}", it, throwable) }
            .let { send(it) }
        null
    }
