package io.github.lengors.webscout.domain.scrapers.models

import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationReturnExtractStock
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationReturnFlatStock
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationReturnStock
import org.apache.commons.jexl3.JexlEngine

@JvmInline
value class ScraperReturnStock private constructor(
    private val stockActions: List<ScraperReturnStockAction>,
) : List<ScraperReturnStockAction> by stockActions {
    constructor(specifications: List<ScraperSpecificationReturnStock>, jexlEngine: JexlEngine) : this(
        specifications.map {
            when (it) {
                is ScraperSpecificationReturnExtractStock -> ScraperReturnExtractStockAction(it, jexlEngine)
                is ScraperSpecificationReturnFlatStock -> ScraperReturnFlatStockAction(it, jexlEngine)
            }
        },
    )
}
