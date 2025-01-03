package io.github.lengors.webscout.domain.scrapers.services

import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponse
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseError
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseErrorCode
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResult
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultBrand
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultDescriptionlessDetail
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultDescriptiveDetail
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecification
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationRequestParser
import io.github.lengors.webscout.domain.channels.runCatching
import io.github.lengors.webscout.domain.collections.asMultiValueMap
import io.github.lengors.webscout.domain.collections.mapEachValue
import io.github.lengors.webscout.domain.events.models.EventListener
import io.github.lengors.webscout.domain.network.http.models.HttpRequest
import io.github.lengors.webscout.domain.network.http.services.HttpStatefulClientBuilder
import io.github.lengors.webscout.domain.network.ssl.services.SslMaterialLoader
import io.github.lengors.webscout.domain.scrapers.contexts.models.ScraperContext
import io.github.lengors.webscout.domain.scrapers.exceptions.ScraperHandlerNotFoundException
import io.github.lengors.webscout.domain.scrapers.exceptions.ScraperInputMissingException
import io.github.lengors.webscout.domain.scrapers.exceptions.ScraperInvalidInputException
import io.github.lengors.webscout.domain.scrapers.models.Scraper
import io.github.lengors.webscout.domain.scrapers.models.ScraperComputeAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperFlatAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperMapAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperPayloadType
import io.github.lengors.webscout.domain.scrapers.models.ScraperRequestAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperReturnAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperTask
import io.github.lengors.webscout.domain.scrapers.specifications.events.ScraperSpecificationEntityBatchDeletedEvent
import io.github.lengors.webscout.domain.scrapers.specifications.events.ScraperSpecificationEntityCreatedEvent
import io.github.lengors.webscout.domain.scrapers.specifications.events.ScraperSpecificationEntityEvent
import io.github.lengors.webscout.domain.scrapers.specifications.events.ScraperSpecificationPersistenceEvent
import io.micrometer.core.instrument.kotlin.asContextElement
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.jexl3.JexlEngine
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class ScraperService(
    private val jexlEngine: JexlEngine,
    private val sslMaterialLoader: SslMaterialLoader,
    private val httpStatefulClientBuilder: HttpStatefulClientBuilder,
    private val observationRegistry: ObservationRegistry,
    @Lazy private val self: ScraperService? = null,
) : EventListener<ScraperSpecificationPersistenceEvent> {
    companion object {
        private val logger = LoggerFactory.getLogger(ScraperService::class.java)
    }

    fun computeScraper(scraperSpecification: ScraperSpecification): Scraper =
        Scraper(scraperSpecification, jexlEngine, httpStatefulClientBuilder, sslMaterialLoader)

    @Cacheable("Scrapers", key = "#scraperSpecification.name")
    fun findScraper(scraperSpecification: ScraperSpecification): Scraper = computeScraper(scraperSpecification)

    @CacheEvict("Scrapers", key = "#scraperSpecification.name")
    fun deleteScraper(scraperSpecification: ScraperSpecification) {
        logger.info("Discarding scraper for ScraperSpecification(name={})", scraperSpecification.name)
    }

    override fun onEvent(event: ScraperSpecificationPersistenceEvent) {
        when (event) {
            is ScraperSpecificationEntityCreatedEvent -> Unit
            is ScraperSpecificationEntityEvent -> self?.deleteScraper(event.entity) ?: Unit
            is ScraperSpecificationEntityBatchDeletedEvent -> event.entities.forEach { self?.deleteScraper(it) }
        }
    }

    fun scrap(vararg scraperTasks: ScraperTask): Flow<ScraperResponse> = scrap(scraperTasks.asFlow())

    fun scrap(scraperTasks: Iterable<ScraperTask>): Flow<ScraperResponse> = scrap(scraperTasks.asFlow())

    fun scrap(scraperTasks: Flow<ScraperTask>): Flow<ScraperResponse> = channelFlow { scrap(scraperTasks) }

    suspend fun SendChannel<ScraperResponse>.scrap(vararg scraperTasks: ScraperTask) = scrap(scraperTasks.asFlow())

    suspend fun SendChannel<ScraperResponse>.scrap(scraperTasks: Iterable<ScraperTask>) = scrap(scraperTasks.asFlow())

    suspend fun SendChannel<ScraperResponse>.scrap(scraperTasks: Flow<ScraperTask>) =
        withContext(observationRegistry.asContextElement()) {
            coroutineScope {
                scraperTasks.collect {
                    launch {
                        scrap(it)
                    }
                }
            }
        }

    private suspend fun SendChannel<ScraperResponse>.scrap(scraperTask: ScraperTask) {
        val scraper = self?.findScraper(scraperTask.specification) ?: return

        val requiredInputs =
            runCatching(logger, {
                when (it) {
                    is ScraperInputMissingException ->
                        ScraperResponseError(
                            ScraperResponseErrorCode.INPUT_MISSING,
                            scraper.name,
                            null,
                            "Input for requirement '${it.requirement.name}' of type '${it.requirement.type}' missing",
                        )

                    is ScraperInvalidInputException ->
                        ScraperResponseError(
                            ScraperResponseErrorCode.INVALID_INPUT,
                            scraper.name,
                            null,
                            "Invalid input for requirement '${it.requirement.name}' of type '${it.requirement.type}'",
                        )

                    else ->
                        ScraperResponseError(
                            ScraperResponseErrorCode.COMPUTE_INPUT,
                            scraper.name,
                            null,
                            it.message,
                        )
                }
            }) {
                scraper.requirementValidators.associate { requirementValidator ->
                    requirementValidator.specification.name to
                        scraperTask.inputs
                            .getOrDefault(
                                requirementValidator.specification.name,
                                requirementValidator.specification.default,
                            ).let(requirementValidator::validate)
                }
            } ?: return

        val scraperContext = ScraperContext(scraper, scraperTask.searchTerm, requiredInputs)
        val defaultGates =
            runCatching(logger, {
                ScraperResponseError(
                    ScraperResponseErrorCode.COMPUTE_DEFAULT_GATES,
                    scraper.name,
                    null,
                    it.message,
                )
            }) {
                with(scraperContext) {
                    scraper.defaultGates
                        .compute(String::class)
                        .mapNotNull { it.valueOrNull }
                        .toSet()
                }
            } ?: return

        scrap(scraperContext.copy(gates = defaultGates))
    }

    private suspend fun SendChannel<ScraperResponse>.scrap(scraperContext: ScraperContext): Unit =
        withContext(observationRegistry.asContextElement()) {
            coroutineScope {
                val scraperHandler =
                    runCatching(logger, {
                        when (it) {
                            is ScraperHandlerNotFoundException ->
                                ScraperResponseError(
                                    ScraperResponseErrorCode.HANDLER_NOT_FOUND,
                                    scraperContext.scraper.name,
                                    null,
                                    "Could not find an handler for specification '${scraperContext.scraper.name}'",
                                )

                            else ->
                                ScraperResponseError(
                                    ScraperResponseErrorCode.COMPUTE_HANDLER,
                                    scraperContext.scraper.name,
                                    null,
                                    it.message,
                                )
                        }
                    }) {
                        with(scraperContext) {
                            scraper.handlers.firstOrNull { handler ->
                                handler.matches?.takeUnless { it.compute(Boolean::class).valueOrNull == true } == null &&
                                    handler.requiresGates
                                        .compute(String::class)
                                        .mapNotNull { it.valueOrNull }
                                        .let(gates::containsAll)
                            }
                        } ?: throw ScraperHandlerNotFoundException(scraperContext.scraper.name)
                    } ?: return@coroutineScope

                logger.info("Scraper handler: (name={})", scraperHandler.name)

                when (scraperHandler.action) {
                    is ScraperFlatAction ->
                        runCatching(logger, {
                            ScraperResponseError(
                                ScraperResponseErrorCode.COMPUTE_FLAT_EXPRESSION,
                                scraperContext.scraper.name,
                                scraperHandler.name,
                                it.message,
                            )
                        }) {
                            with(scraperContext) {
                                scraperHandler.action.jexlExpression
                                    .compute(List::class)
                                    .valueOrNull
                            }
                        }?.let { actions ->
                            actions.forEach {
                                launch {
                                    scrap(
                                        scraperContext.branch(
                                            visitedHandlerName = scraperHandler.name,
                                            openGates = scraperHandler.opensGates,
                                            closeGates = scraperHandler.closesGates,
                                            valueOrNull = it,
                                        ),
                                    )
                                }
                            }
                        }

                    is ScraperComputeAction ->
                        with(scraperContext) {
                            val mappedValues =
                                runCatching(logger, {
                                    ScraperResponseError(
                                        ScraperResponseErrorCode.COMPUTE_MAPS_EXPRESSION,
                                        scraperContext.scraper.name,
                                        scraperHandler.name,
                                        it.message,
                                    )
                                }) {
                                    scraperHandler.action.maps
                                        .compute()
                                        .mapNotNull { it.valueOrNull }
                                } ?: return@coroutineScope

                            val requestValueHolder =
                                when (scraperHandler.action) {
                                    is ScraperMapAction -> null
                                    is ScraperRequestAction ->
                                        scraperHandler.action.let { request ->
                                            val httpRequest =
                                                runCatching(logger, {
                                                    ScraperResponseError(
                                                        ScraperResponseErrorCode.COMPUTE_REQUEST,
                                                        scraperContext.scraper.name,
                                                        scraperHandler.name,
                                                        it.message,
                                                    )
                                                }) {
                                                    val defaultUriComponents = scraper.defaultUrl.computeUri()
                                                    val uriComponents = request.url.computeUri(defaultUriComponents)

                                                    val uri = uriComponents.toUri()
                                                    val defaultHeaders =
                                                        scraper.defaultHeaders
                                                            .compute(String::class)
                                                            .mapEachValue { it.valueOrNull }
                                                    val headers =
                                                        request.headers
                                                            .compute(String::class)
                                                            .mapEachValue { it.valueOrNull }
                                                    val fields =
                                                        request.payload
                                                            ?.fields
                                                            ?.compute(String::class)
                                                            ?.mapEachValue { it.valueOrNull }
                                                            ?.asMultiValueMap()
                                                    val contentType =
                                                        request.payload?.let { payload ->
                                                            when (payload.type) {
                                                                ScraperPayloadType.DATA -> MediaType.APPLICATION_FORM_URLENCODED
                                                                ScraperPayloadType.JSON -> MediaType.APPLICATION_JSON
                                                            }
                                                        }
                                                    val accept =
                                                        when (request.parser) {
                                                            ScraperSpecificationRequestParser.HTML -> MediaType.TEXT_HTML
                                                            ScraperSpecificationRequestParser.JSON -> MediaType.APPLICATION_JSON
                                                            ScraperSpecificationRequestParser.TEXT -> MediaType.TEXT_PLAIN
                                                        }

                                                    HttpRequest(
                                                        uri,
                                                        request.method,
                                                        defaultHeaders + headers +
                                                            mapOf(
                                                                HttpHeaders.ACCEPT to "$accept",
                                                                HttpHeaders.CONTENT_TYPE to "$contentType",
                                                            ),
                                                        fields,
                                                    )
                                                } ?: return@coroutineScope

                                            runCatching(logger, {
                                                ScraperResponseError(
                                                    ScraperResponseErrorCode.COMPUTE_RESPONSE,
                                                    scraperContext.scraper.name,
                                                    scraperHandler.name,
                                                    it.message,
                                                )
                                            }) {
                                                val response = scraper.httpStatefulClient.exchange(httpRequest)
                                                val responseBodyContext = copy(valueOrNull = response.body)
                                                response.uri to
                                                    when (request.parser) {
                                                        ScraperSpecificationRequestParser.HTML -> responseBodyContext.html()
                                                        ScraperSpecificationRequestParser.JSON -> responseBodyContext.json()
                                                        ScraperSpecificationRequestParser.TEXT -> responseBodyContext
                                                    }
                                            } ?: return@coroutineScope
                                        }
                                }

                            val requestValues = requestValueHolder?.second?.valueOrNull?.let(::listOf) ?: emptyList()
                            requestValueHolder?.first to requestValues + mappedValues
                        }.let { (uri, output) ->
                            scrap(
                                scraperContext.branch(
                                    visitedHandlerName = scraperHandler.name,
                                    openGates = scraperHandler.opensGates,
                                    closeGates = scraperHandler.closesGates,
                                    uri =
                                        uri?.let {
                                            UriComponentsBuilder
                                                .fromUri(it)
                                                .build()
                                        },
                                    valueOrNull = if (output.size == 1) output[0] else output,
                                ),
                            )
                        }

                    is ScraperReturnAction ->
                        send(
                            runCatching(logger, {
                                ScraperResponseError(
                                    ScraperResponseErrorCode.COMPUTE_RETURN,
                                    scraperContext.scraper.name,
                                    scraperHandler.name,
                                    it.message,
                                )
                            }) {
                                with(scraperContext) {
                                    ScraperResponseResult(
                                        scraper.defaultUrl
                                            .computeUri()
                                            .toUriString(),
                                        scraper.name,
                                        scraperHandler.action.description.computeDescription(),
                                        ScraperResponseResultBrand(
                                            scraperHandler.action.brand.description
                                                .computeBrand(),
                                            scraperHandler.action.brand.image
                                                .computeUriStringOrNull(),
                                        ),
                                        scraperHandler.action.price.computePriceAsync(),
                                        scraperHandler.action.image.computeUriStringOrNull(),
                                        scraperHandler.action.stocks.computeStocksAsync(),
                                        scraperHandler.action.grip.computeGradingOrNull(),
                                        scraperHandler.action.noise.computeNoiseLevelOrNull(),
                                        scraperHandler.action.decibels.computeDecibelsOrNull(),
                                        scraperHandler.action.consumption.computeGradingOrNull(),
                                        scraperHandler.action.details.mapNotNull {
                                            it.name
                                                .computeTextOrNull()
                                                ?.let { name ->
                                                    it.description
                                                        .computeTextOrNull()
                                                        ?.let { description ->
                                                            ScraperResponseResultDescriptiveDetail(
                                                                name,
                                                                description,
                                                                it.image.computeUriStringOrNull(),
                                                            )
                                                        }
                                                        ?: it.image
                                                            .computeUriStringOrNull()
                                                            ?.let { image ->
                                                                ScraperResponseResultDescriptionlessDetail(
                                                                    name,
                                                                    image,
                                                                )
                                                            }
                                                }
                                        },
                                    )
                                }
                            }?.also { logger.info("Emitted {}", it.description) } ?: return@coroutineScope,
                        )
                }
            }
        }
}
