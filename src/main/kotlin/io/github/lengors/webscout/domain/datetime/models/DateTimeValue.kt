package io.github.lengors.webscout.domain.datetime.models

import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultDateTime

sealed interface DateTimeValue {
    fun toScraperResponseResultDateTime(): ScraperResponseResultDateTime
}
