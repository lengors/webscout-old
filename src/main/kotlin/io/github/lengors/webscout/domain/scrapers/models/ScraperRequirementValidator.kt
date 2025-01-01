package io.github.lengors.webscout.domain.scrapers.models

import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationRequirement
import io.github.lengors.protoscout.domain.scrapers.specifications.models.ScraperSpecificationRequirementType
import io.github.lengors.webscout.domain.scrapers.exceptions.ScraperInputMissingException
import io.github.lengors.webscout.domain.scrapers.exceptions.ScraperInvalidInputException
import io.github.lengors.webscout.domain.validators.EmailValidator

@JvmInline
value class ScraperRequirementValidator(
    val specification: ScraperSpecificationRequirement,
) {
    fun validate(input: String?): String {
        input ?: throw ScraperInputMissingException(specification)
        val validation =
            when (specification.type) {
                ScraperSpecificationRequirementType.PASSWORD, ScraperSpecificationRequirementType.TEXT -> null
                ScraperSpecificationRequirementType.EMAIL -> EmailValidator.isValid(input)
            }
        return input
            .takeIf { validation != false }
            ?: throw ScraperInvalidInputException(input, specification)
    }
}
