package io.github.lengors.webscout.domain.scrapers.models

import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationDataPayload
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationJsonPayload
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationPayload
import io.github.lengors.webscout.domain.collections.mapEachValue
import io.github.lengors.webscout.domain.jexl.services.createExpression
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlExpression

@ConsistentCopyVisibility
data class ScraperPayload private constructor(
    val fields: Map<String, JexlExpression>,
    val type: ScraperPayloadType,
) {
    constructor(specification: ScraperSpecificationPayload, jexlEngine: JexlEngine) : this(
        when (specification) {
            is ScraperSpecificationDataPayload -> specification.data.additionalProperties
            is ScraperSpecificationJsonPayload -> specification.json.additionalProperties
        }.mapEachValue(jexlEngine::createExpression),
        when (specification) {
            is ScraperSpecificationDataPayload -> ScraperPayloadType.DATA
            is ScraperSpecificationJsonPayload -> ScraperPayloadType.JSON
        },
    )
}
