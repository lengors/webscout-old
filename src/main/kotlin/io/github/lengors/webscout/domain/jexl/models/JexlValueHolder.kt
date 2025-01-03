package io.github.lengors.webscout.domain.jexl.models

import com.fasterxml.jackson.databind.JsonNode
import io.github.lengors.webscout.domain.datetime.models.DateTimeInstantValue
import io.github.lengors.webscout.domain.datetime.models.DateTimeRangeValue
import io.github.lengors.webscout.domain.datetime.models.DateTimeValue
import io.github.lengors.webscout.domain.qualities.models.Grading
import io.github.lengors.webscout.domain.qualities.models.NoiseLevel
import io.github.lengors.webscout.domain.quantities.models.Quantity
import io.github.lengors.webscout.domain.quantities.models.QuantityModifier
import io.github.lengors.webscout.domain.regex.decibels
import io.github.lengors.webscout.domain.regex.grading
import io.github.lengors.webscout.domain.regex.noiseLevel
import io.github.lengors.webscout.domain.regex.quantity
import io.github.lengors.webscout.domain.regex.supplementary
import io.github.lengors.webscout.domain.regex.whitespace
import io.github.lengors.webscout.integrations.duckling.models.DucklingAmountOfMoneyRequest
import io.github.lengors.webscout.integrations.duckling.models.DucklingDateTimeInstantResponseValue
import io.github.lengors.webscout.integrations.duckling.models.DucklingDateTimeRangeResponseValue
import io.github.lengors.webscout.integrations.duckling.models.DucklingDateTimeRequest
import org.javamoney.moneta.Money
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.LeafNode
import org.jsoup.nodes.Node
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import java.util.Currency
import javax.money.MonetaryAmount

interface JexlValueHolder<out T : Any> {
    fun at(index: Int): JexlValueHolder<Any> =
        flatMap {
            when (it) {
                is List<*> -> it[index]
                is Iterable<*> -> it.toList()[index]
                else -> it.takeIf { index == 0 }
            }
        }

    fun attr(attribute: String?): JexlValueHolder<String> =
        flatMap { value ->
            attribute
                ?.let { attr ->
                    when (value) {
                        is Node -> value.attr(attr)
                        else -> string().valueOrNull?.takeIf { attr == it }
                    }
                }
                ?: when (value) {
                    is String -> value
                    is LeafNode -> value.attr(value.nodeName())
                    is Element -> value.text()
                    is JsonNode -> value.asText()
                    is MonetaryAmount -> "${value.number}${Currency.getInstance(value.currency.currencyCode).symbol}"
                    else -> value.toString()
                }
        }

    fun brand(): JexlValueHolder<String> = map { it.replace(Regex.supplementary, "") }

    val context: JexlContextValueHolder

    fun date(): JexlValueHolder<DateTimeValue> =
        flatMap {
            when (it) {
                is DateTimeValue -> it
                else ->
                    map { text ->
                        context.ducklingClient
                            .parse(DucklingDateTimeRequest(text, context.locale, context.timezone))
                            ?.value
                            ?.let { responseValue ->
                                when (responseValue) {
                                    is DucklingDateTimeInstantResponseValue -> DateTimeInstantValue(responseValue)
                                    is DucklingDateTimeRangeResponseValue -> DateTimeRangeValue(responseValue)
                                }
                            }
                    }.valueOrNull
            }
        }

    fun decibels(): JexlValueHolder<Int> =
        map {
            Regex.decibels
                .find(it)
                ?.groups
                ?.get(1)
                ?.value
                ?.toIntOrNull()
        }

    fun description(): JexlValueHolder<String> = text().map(String::uppercase)

    fun <U : Any> flatMap(action: (Any) -> U?): JexlValueHolder<U> =
        valueOrNull.let {
            when (it) {
                is JexlValueHolder<*> -> it.flatMap(action)
                else -> JexlSimpleValueHolder(context, it?.let(action))
            }
        }

    fun grading(): JexlValueHolder<Grading> =
        map { stringValue ->
            Regex.grading
                .find(stringValue)
                ?.value
                ?.let { grading ->
                    grading
                        .toIntOrNull()
                        ?.let { Grading.entries[it - 1] }
                        ?: Grading.valueOrNull(grading.uppercase())
                }
        }

    fun html(): JexlValueHolder<Node> =
        flatMap {
            when (it) {
                is Node -> it
                else -> map(Jsoup::parse).valueOrNull
            }
        }

    fun json(): JexlValueHolder<JsonNode> =
        flatMap {
            when (it) {
                is JsonNode -> it
                else -> map(context.objectMapper::readTree).valueOrNull
            }
        }

    fun <U : Any> map(action: (String) -> U?): JexlValueHolder<U> = JexlSimpleValueHolder(context, string().valueOrNull?.let(action))

    fun noiseLevel(): JexlValueHolder<NoiseLevel> =
        map { stringValue ->
            Regex.noiseLevel
                .find(stringValue)
                ?.value
                ?.let { noiseLevel ->
                    noiseLevel
                        .toIntOrNull()
                        ?.let { NoiseLevel.entries[it - 1] }
                        ?: NoiseLevel.valueOrNull(noiseLevel.uppercase())
                }
        }

    fun price(): JexlValueHolder<MonetaryAmount> =
        flatMap {
            when (it) {
                is MonetaryAmount -> it
                else ->
                    map { stringValue ->
                        context.ducklingClient
                            .parse(DucklingAmountOfMoneyRequest(stringValue, context.locale, context.timezone))
                            ?.let { response -> Money.of(response.value.value, response.value.unit) }
                    }.valueOrNull
            }
        }

    fun quantity(): JexlValueHolder<Quantity> =
        flatMap {
            when (it) {
                is Quantity -> it
                else ->
                    map { stringValue ->
                        Regex.quantity
                            .find(stringValue)
                            ?.let { quantityMatch ->
                                Quantity(
                                    quantityMatch.groups[2]?.value?.toInt() ?: 0,
                                    when (quantityMatch.groups[1]?.value) {
                                        "+", ">" -> QuantityModifier.AT_LEAST
                                        "-", "<" -> QuantityModifier.AT_MOST
                                        else -> QuantityModifier.EXACT
                                    },
                                )
                            }
                    }.valueOrNull
            }
        }

    fun select(selector: String): JexlValueHolder<Any> =
        JexlSimpleValueHolder(
            context,
            selectAll(selector).valueOrNull?.firstOrNull(),
        )

    fun selectAll(selector: String): JexlValueHolder<Iterable<*>> =
        flatMap {
            when (it) {
                is JsonNode -> it.at(selector)
                is Element -> it.selectXpath(selector, Node::class.java)
                is Iterable<*> ->
                    it.flatMap { value ->
                        JexlSimpleValueHolder(context, value)
                            .selectAll(selector)
                            .valueOrNull
                            ?: emptyList()
                    }

                else -> attr(selector).valueOrNull?.let(::listOf)
            }
        }

    fun string(): JexlValueHolder<String> = attr(null)

    fun text(): JexlValueHolder<String> =
        map { stringValue ->
            stringValue
                .trim()
                .replace(Regex.whitespace, " ")
        }

    fun uri(): JexlValueHolder<UriComponents> =
        map { stringValue ->
            UriComponentsBuilder
                .fromUriString(stringValue)
                .build()
                .let { selectedUri ->
                    context.uri
                        ?.let {
                            UriComponentsBuilder
                                .newInstance()
                                .uriComponents(it)
                        }?.uriComponents(selectedUri)
                        ?.build()
                        ?: selectedUri
                }
        }

    val value: T
        get() = valueOrNull ?: throw NoSuchElementException("No value exists")

    val valueOrNull: T?
}

suspend fun <T : Any> JexlValueHolder<T>.dateAsync(): JexlValueHolder<DateTimeValue> =
    flatMapAsync {
        when (it) {
            is DateTimeValue -> it
            else ->
                mapAsync { text ->
                    context.ducklingClient
                        .parseAsync(DucklingDateTimeRequest(text, context.locale, context.timezone))
                        ?.value
                        ?.let { responseValue ->
                            when (responseValue) {
                                is DucklingDateTimeInstantResponseValue -> DateTimeInstantValue(responseValue)
                                is DucklingDateTimeRangeResponseValue -> DateTimeRangeValue(responseValue)
                            }
                        }
                }.valueOrNull
        }
    }

suspend fun <T : Any, U : Any> JexlValueHolder<T>.flatMapAsync(action: suspend (Any) -> U?): JexlValueHolder<U> =
    valueOrNull.let {
        when (it) {
            is JexlValueHolder<*> -> it.flatMapAsync(action)
            else -> JexlSimpleValueHolder(context, it?.let { action(it) })
        }
    }

suspend fun <T : Any, U : Any> JexlValueHolder<T>.mapAsync(action: suspend (String) -> U?): JexlValueHolder<U> =
    JexlSimpleValueHolder(context, string().valueOrNull?.let { action(it) })

suspend fun <T : Any> JexlValueHolder<T>.priceAsync(): JexlValueHolder<MonetaryAmount> =
    flatMapAsync {
        when (it) {
            is MonetaryAmount -> it
            else ->
                mapAsync { stringValue ->
                    context.ducklingClient
                        .parseAsync(DucklingAmountOfMoneyRequest(stringValue, context.locale, context.timezone))
                        ?.let { response -> Money.of(response.value.value, response.value.unit) }
                }.valueOrNull
        }
    }
