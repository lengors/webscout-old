package io.github.lengors.webscout.domain.jexl.models

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.lengors.webscout.domain.application.services.ApplicationService
import io.github.lengors.webscout.domain.application.services.getBean
import io.github.lengors.webscout.domain.collections.mapEachValue
import io.github.lengors.webscout.integrations.duckling.client.DucklingClient
import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.JexlExpression
import org.springframework.web.util.UriComponents
import java.time.ZoneId
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface JexlContextValueHolder :
    JexlContext,
    JexlValueHolder<Any> {
    fun <T : Any> JexlExpression?.compute(type: KClass<T>): JexlValueHolder<T> =
        this
            ?.evaluate(this@JexlContextValueHolder)
            .let {
                JexlSimpleValueHolder(
                    this@JexlContextValueHolder,
                    when (it) {
                        is JexlValueHolder<*> -> it.valueOrNull
                        else -> it
                    }?.let(type::cast),
                )
            }

    fun JexlExpression?.compute(): JexlValueHolder<Any> = compute(Any::class)

    fun <T : Any> Iterable<JexlExpression>?.compute(type: KClass<T>): List<JexlValueHolder<T>> =
        this
            ?.map { it.compute(type) }
            ?: emptyList()

    fun Iterable<JexlExpression>?.compute(): List<JexlValueHolder<Any>> = compute(Any::class)

    fun <T : Any> Map<String, JexlExpression>?.compute(type: KClass<T>): Map<String, JexlValueHolder<T>> =
        this
            ?.mapEachValue { it.compute(type) }
            ?: emptyMap()

    fun Map<String, JexlExpression>?.compute(): Map<String, JexlValueHolder<Any>> = compute(Any::class)

    override val context: JexlContextValueHolder
        get() = this

    val ducklingClient: DucklingClient
        get() = ApplicationService.getBean()

    fun join(
        delimiter: String,
        vararg values: Any?,
    ): String =
        values
            .mapNotNull {
                when (it) {
                    is JexlValueHolder<*> -> it.string().valueOrNull
                    else -> it?.toString()
                }
            }.joinToString(delimiter)

    val locale: Locale?
        get() = null

    val objectMapper: ObjectMapper
        get() = ApplicationService.getBean()

    val timezone: ZoneId?
        get() = null

    val uri: UriComponents?
        get() = null
}
