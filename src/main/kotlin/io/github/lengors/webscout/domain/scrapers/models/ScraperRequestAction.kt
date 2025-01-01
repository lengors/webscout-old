package io.github.lengors.webscout.domain.scrapers.models

import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationRequestAction
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationRequestParser
import io.github.lengors.webscout.domain.collections.mapEachValue
import io.github.lengors.webscout.domain.jexl.services.createExpression
import io.github.lengors.webscout.domain.network.http.models.HttpMethod
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlExpression

@ConsistentCopyVisibility
data class ScraperRequestAction private constructor(
    val url: ScraperUrl,
    val method: HttpMethod,
    val parser: ScraperSpecificationRequestParser,
    val payload: ScraperPayload? = null,
    val headers: Map<String, JexlExpression> = emptyMap(),
    override val maps: List<JexlExpression> = emptyList(),
) : ScraperComputeAction {
    constructor(specification: ScraperSpecificationRequestAction, jexlEngine: JexlEngine) : this(
        ScraperUrl(specification.requests.url, jexlEngine),
        HttpMethod.valueOf(specification.requests.method.name),
        specification.requests.parser,
        specification.requests.payload?.let { ScraperPayload(it, jexlEngine) },
        specification.requests.headers
            ?.additionalProperties
            ?.mapEachValue(jexlEngine::createExpression)
            ?: emptyMap(),
        specification.maps?.map(jexlEngine::createExpression) ?: emptyList(),
    )
}
