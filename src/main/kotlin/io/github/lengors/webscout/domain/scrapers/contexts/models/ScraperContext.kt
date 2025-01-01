package io.github.lengors.webscout.domain.scrapers.contexts.models

import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultDateTime
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultGrading
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultNoiseLevel
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultPrice
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultQuantity
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultQuantityModifier
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultStock
import io.github.lengors.webscout.domain.jexl.models.JexlContextValueHolder
import io.github.lengors.webscout.domain.jexl.models.JexlValueHolder
import io.github.lengors.webscout.domain.jexl.models.dateAsync
import io.github.lengors.webscout.domain.jexl.models.priceAsync
import io.github.lengors.webscout.domain.scrapers.models.Scraper
import io.github.lengors.webscout.domain.scrapers.models.ScraperReturnExtractStockAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperReturnFlatStockAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperReturnStockAction
import io.github.lengors.webscout.domain.scrapers.models.ScraperUrl
import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.JexlExpression
import org.apache.commons.jexl3.ObjectContext
import org.springframework.util.CollectionUtils
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import java.time.ZoneId
import java.util.Locale

data class ScraperContext(
    val scraper: Scraper,
    val searchTerm: String,
    val inputs: Map<String, String> = emptyMap(),
    val visitedHandlers: List<String> = emptyList(),
    val gates: Set<String> = emptySet(),
    override val uri: UriComponents? = null,
    override val valueOrNull: Any? = null,
) : JexlContextValueHolder {
    private val jexlContext: JexlContext = ObjectContext(scraper.jexlEngine, this)

    fun branch(
        visitedHandlerName: String,
        openGates: List<JexlExpression>,
        closeGates: List<JexlExpression>,
        uri: UriComponents? = null,
        valueOrNull: Any? = null,
    ): ScraperContext =
        ScraperContext(
            scraper = scraper,
            searchTerm = searchTerm,
            inputs = inputs,
            visitedHandlers = visitedHandlers + visitedHandlerName,
            gates =
                gates +
                    openGates
                        .compute(String::class)
                        .mapNotNull { it.valueOrNull }
                        .toSet() -
                    closeGates
                        .compute(String::class)
                        .mapNotNull { it.valueOrNull }
                        .toSet(),
            uri = uri ?: this.uri,
            valueOrNull = valueOrNull,
        )

    fun JexlExpression.computeBrand(): String =
        this
            .compute()
            .brand()
            .value

    suspend fun JexlExpression?.computeDateOrNullAsync(): ScraperResponseResultDateTime? =
        this
            .compute()
            .dateAsync()
            .valueOrNull
            ?.toScraperResponseResultDateTime()

    fun JexlExpression?.computeDecibelsOrNull(): Int? =
        this
            .compute()
            .decibels()
            .valueOrNull

    fun JexlExpression.computeDescription(): String =
        this
            .compute()
            .description()
            .value

    fun JexlExpression?.computeGradingOrNull(): ScraperResponseResultGrading? =
        this
            .compute()
            .grading()
            .valueOrNull
            ?.name
            ?.let(ScraperResponseResultGrading::valueOf)

    fun JexlExpression?.computeNoiseLevelOrNull(): ScraperResponseResultNoiseLevel? =
        this
            .compute()
            .noiseLevel()
            .valueOrNull
            ?.name
            ?.let(ScraperResponseResultNoiseLevel::valueOf)

    suspend fun JexlExpression.computePriceAsync(): ScraperResponseResultPrice =
        this
            .compute()
            .priceAsync()
            .value
            .let { ScraperResponseResultPrice(it.number.toString(), it.currency.currencyCode) }

    fun JexlExpression.computeQuantity(): ScraperResponseResultQuantity =
        this
            .compute()
            .quantity()
            .value
            .let {
                ScraperResponseResultQuantity(
                    it.amount,
                    ScraperResponseResultQuantityModifier.valueOf(it.modifier.name),
                )
            }

    suspend fun ScraperReturnExtractStockAction.computeStockAsync(): List<ScraperResponseResultStock> =
        listOf(
            ScraperResponseResultStock(
                availability.computeQuantity(),
                storage.computeTextOrNull(),
                deliveryDateTime.computeDateOrNullAsync(),
            ),
        )

    suspend fun List<ScraperReturnStockAction>.computeStocksAsync(): List<ScraperResponseResultStock> =
        flatMap {
            when (it) {
                is ScraperReturnExtractStockAction -> it.computeStockAsync()
                is ScraperReturnFlatStockAction ->
                    it.flattens
                        .compute(List::class)
                        .valueOrNull
                        ?.flatMap { value ->
                            with(this@ScraperContext.copy(valueOrNull = value)) {
                                it.extracts.computeStocksAsync()
                            }
                        }
                        ?: emptyList()
            }
        }

    fun JexlExpression?.computeTextOrNull(): String? =
        this
            .compute()
            .text()
            .valueOrNull

    fun ScraperUrl.computeUri(defaultUri: UriComponents? = null): UriComponents =
        UriComponentsBuilder
            .newInstance()
            .let { defaultUri?.let(it::uriComponents) ?: it }
            .let {
                location
                    .computeUriOrNull()
                    ?.let(it::uriComponents)
                    ?: it
            }.let {
                scheme
                    .compute(String::class)
                    .valueOrNull
                    ?.let(it::scheme)
                    ?: it
            }.let {
                host
                    .compute(String::class)
                    .valueOrNull
                    ?.let(it::host)
                    ?: it
            }.let {
                path
                    .compute(String::class)
                    .valueOrNull
                    ?.let(it::path)
                    ?: it
            }.let { builder ->
                parameters
                    ?.let { nonNullParameters ->
                        builder.replaceQueryParams(
                            CollectionUtils.toMultiValueMap(
                                nonNullParameters
                                    .map {
                                        it.key.compute(String::class).value to
                                            it.value
                                                .compute(String::class)
                                                .map { valueHolder -> valueHolder.valueOrNull }
                                    }.toMap(),
                            ),
                        )
                    }
                    ?: builder
            }.build()

    fun JexlExpression?.computeUri(): JexlValueHolder<UriComponents> =
        this
            .compute()
            .uri()

    fun JexlExpression?.computeUriOrNull(): UriComponents? = computeUri().valueOrNull

    fun JexlExpression?.computeUriStringOrNull(): String? =
        this
            .computeUri()
            .string()
            .valueOrNull

    override fun get(name: String?): Any? = jexlContext.get(name)

    override fun has(name: String?): Boolean = jexlContext.has(name)

    override val locale: Locale by lazy {
        scraper.locale
            .compute()
            .value
            .let {
                when (it) {
                    is String -> Locale.forLanguageTag(it)
                    is Locale -> it
                    else -> throw IllegalArgumentException("Unsupported locale: $it")
                }
            }
    }

    override fun set(
        name: String?,
        value: Any?,
    ) = jexlContext.set(name, value)

    override val timezone: ZoneId by lazy {
        scraper.timezone
            .compute()
            .value
            .let {
                when (it) {
                    is String -> ZoneId.of(it)
                    is ZoneId -> it
                    else -> throw IllegalArgumentException("Unsupported timezone: $it")
                }
            }
    }
}
