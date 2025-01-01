package io.github.lengors.webscout.domain.spring.r2dbc.configurations

import io.github.lengors.webscout.domain.converters.ConverterSpecification
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration

@Configuration(proxyBeanMethods = false)
class R2dbcConfiguration(
    private val r2dbcProperties: R2dbcProperties,
    private val converterSpecifications: List<ConverterSpecification<*, *>>,
) : AbstractR2dbcConfiguration() {
    override fun connectionFactory(): ConnectionFactory = ConnectionFactories.get(r2dbcProperties.url)

    override fun getCustomConverters(): List<Any> =
        converterSpecifications.flatMap {
            listOf(it.readingConverterSupplier(), it.writingConverterSupplier())
        }
}
