package io.github.lengors.webscout.domain.scrapers.models

import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationReturnAction
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationReturnDescriptionlessDetail
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationReturnDescriptiveDetail
import io.github.lengors.webscout.domain.jexl.services.createExpression
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlExpression

@ConsistentCopyVisibility
data class ScraperReturnAction private constructor(
    val description: JexlExpression,
    val brand: ScraperReturnBrand,
    val price: JexlExpression,
    val image: JexlExpression? = null,
    val stocks: List<ScraperReturnStockAction> = emptyList(),
    val grip: JexlExpression? = null,
    val noise: JexlExpression? = null,
    val decibels: JexlExpression? = null,
    val consumption: JexlExpression? = null,
    val details: List<ScraperReturnDetail> = emptyList(),
) : ScraperAction {
    constructor(specification: ScraperSpecificationReturnAction, jexlEngine: JexlEngine) : this(
        jexlEngine.createExpression(specification.returns.description),
        ScraperReturnBrand(specification.returns.brand, jexlEngine),
        jexlEngine.createExpression(specification.returns.price),
        specification.returns.image?.let(jexlEngine::createExpression),
        specification.returns.stocks?.let { ScraperReturnStock(it, jexlEngine) } ?: emptyList(),
        specification.returns.grip?.let(jexlEngine::createExpression),
        specification.returns.noise?.let(jexlEngine::createExpression),
        specification.returns.decibels?.let(jexlEngine::createExpression),
        specification.returns.consumption?.let(jexlEngine::createExpression),
        specification.returns.details?.map {
            when (it) {
                is ScraperSpecificationReturnDescriptiveDetail -> ScraperReturnDescriptiveDetail(it, jexlEngine)
                is ScraperSpecificationReturnDescriptionlessDetail -> ScraperReturnVisualDetail(it, jexlEngine)
            }
        } ?: emptyList(),
    )
}
