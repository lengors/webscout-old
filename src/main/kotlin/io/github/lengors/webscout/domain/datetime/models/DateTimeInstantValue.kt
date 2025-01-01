package io.github.lengors.webscout.domain.datetime.models

import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultDateTimeInstant
import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultDateTimeInstantGrain
import io.github.lengors.webscout.integrations.duckling.models.DucklingDateTimeInstantResponseValue
import java.time.ZonedDateTime
import java.util.Date

data class DateTimeInstantValue(
    val value: ZonedDateTime,
    val grain: DateTimeGrain,
) : DateTimeValue {
    constructor(ducklingDateTimeInstantResponseValue: DucklingDateTimeInstantResponseValue) : this(
        ducklingDateTimeInstantResponseValue.value,
        DateTimeGrain.valueOf(ducklingDateTimeInstantResponseValue.grain.name),
    )

    override fun toScraperResponseResultDateTime(): ScraperResponseResultDateTimeInstant =
        ScraperResponseResultDateTimeInstant(
            Date.from(value.toInstant()),
            ScraperResponseResultDateTimeInstantGrain.valueOf(grain.name),
        )
}
