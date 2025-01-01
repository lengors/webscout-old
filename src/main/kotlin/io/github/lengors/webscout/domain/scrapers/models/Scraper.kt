package io.github.lengors.webscout.domain.scrapers.models

import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecification
import io.github.lengors.webscout.domain.collections.mapEachValue
import io.github.lengors.webscout.domain.jexl.services.createExpression
import io.github.lengors.webscout.domain.network.http.services.HttpStatefulClient
import io.github.lengors.webscout.domain.network.http.services.HttpStatefulClientBuilder
import io.github.lengors.webscout.domain.network.ssl.services.SslMaterialLoader
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlExpression

@ConsistentCopyVisibility
data class Scraper private constructor(
    val name: String,
    val jexlEngine: JexlEngine,
    val httpStatefulClient: HttpStatefulClient,
    val locale: JexlExpression,
    val timezone: JexlExpression,
    val defaultUrl: ScraperUrl,
    val defaultHeaders: Map<String, JexlExpression> = emptyMap(),
    val defaultGates: List<JexlExpression> = emptyList(),
    val requirementValidators: List<ScraperRequirementValidator> = emptyList(),
    val handlers: List<ScraperHandler> = emptyList(),
) {
    constructor(
        specification: ScraperSpecification,
        jexlEngine: JexlEngine,
        httpStatefulClient: HttpStatefulClientBuilder,
        sslMaterialLoader: SslMaterialLoader,
    ) : this(
        specification.name,
        jexlEngine,
        httpStatefulClient.build(sslMaterialLoader.loadCertificates(specification.settings.certificates)),
        jexlEngine.createExpression(specification.settings.locale),
        jexlEngine.createExpression(specification.settings.timezone),
        ScraperUrl(specification.settings.defaults.url, jexlEngine),
        specification.settings.defaults.headers
            ?.additionalProperties
            ?.mapEachValue(jexlEngine::createExpression)
            ?: emptyMap(),
        specification.settings.defaults.gates
            .map(jexlEngine::createExpression),
        specification.settings.requirements?.map(::ScraperRequirementValidator) ?: emptyList(),
        specification.handlers.map { ScraperHandler(it, jexlEngine) },
    )
}
