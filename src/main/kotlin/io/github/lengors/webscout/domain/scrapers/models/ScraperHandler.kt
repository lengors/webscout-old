package io.github.lengors.webscout.domain.scrapers.models

import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationFlatAction
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationHandler
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationMapAction
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationRequestAction
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationReturnAction
import io.github.lengors.webscout.domain.jexl.services.createExpression
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlExpression

@ConsistentCopyVisibility
data class ScraperHandler private constructor(
    val name: String,
    val action: ScraperAction,
    val opensGates: List<JexlExpression> = emptyList(),
    val closesGates: List<JexlExpression> = emptyList(),
    val requiresGates: List<JexlExpression> = emptyList(),
    val matches: JexlExpression? = null,
) {
    constructor(specification: ScraperSpecificationHandler, jexlEngine: JexlEngine) : this(
        specification.name,
        specification.action.let {
            when (it) {
                is ScraperSpecificationFlatAction -> ScraperFlatAction(it, jexlEngine)
                is ScraperSpecificationMapAction -> ScraperMapAction(it, jexlEngine)
                is ScraperSpecificationRequestAction -> ScraperRequestAction(it, jexlEngine)
                is ScraperSpecificationReturnAction -> ScraperReturnAction(it, jexlEngine)
            }
        },
        specification.gates.opens?.map(jexlEngine::createExpression) ?: emptyList(),
        specification.gates.closes?.map(jexlEngine::createExpression) ?: emptyList(),
        specification.gates.requires.map(jexlEngine::createExpression),
        specification.matches?.let(jexlEngine::createExpression),
    )
}
