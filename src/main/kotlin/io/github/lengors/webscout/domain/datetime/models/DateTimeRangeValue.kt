package io.github.lengors.webscout.domain.datetime.models

import io.github.lengors.protoscout.domain.scrapers.models.ScraperResponseResultDateTimeRange
import io.github.lengors.webscout.integrations.duckling.models.DucklingDateTimeRangeResponseValue

data class DateTimeRangeValue(
    val from: DateTimeInstantValue,
    val to: DateTimeInstantValue,
) : DateTimeValue {
    constructor(ducklingDateTimeRangeResponseValue: DucklingDateTimeRangeResponseValue) : this(
        DateTimeInstantValue(ducklingDateTimeRangeResponseValue.from),
        DateTimeInstantValue(ducklingDateTimeRangeResponseValue.to),
    )

    override fun toScraperResponseResultDateTime(): ScraperResponseResultDateTimeRange =
        ScraperResponseResultDateTimeRange(
            from.toScraperResponseResultDateTime(),
            to.toScraperResponseResultDateTime(),
        )
}
