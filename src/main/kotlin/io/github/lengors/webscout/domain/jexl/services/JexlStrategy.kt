package io.github.lengors.webscout.domain.jexl.services

import org.apache.commons.jexl3.JexlOperator
import org.apache.commons.jexl3.introspection.JexlUberspect
import org.apache.commons.jexl3.introspection.JexlUberspect.PropertyResolver
import org.apache.commons.jexl3.introspection.JexlUberspect.ResolverStrategy

object JexlStrategy : ResolverStrategy {
    private val mapResolvers =
        listOf(
            JexlUberspect.JexlResolver.PROPERTY,
            JexlUberspect.JexlResolver.MAP,
            JexlUberspect.JexlResolver.LIST,
            DuckPropertyResolver,
            JexlUberspect.JexlResolver.FIELD,
            JexlUberspect.JexlResolver.CONTAINER,
        )

    private val pojoResolves =
        listOf(
            JexlUberspect.JexlResolver.MAP,
            JexlUberspect.JexlResolver.LIST,
            DuckPropertyResolver,
            JexlUberspect.JexlResolver.PROPERTY,
            JexlUberspect.JexlResolver.FIELD,
            JexlUberspect.JexlResolver.CONTAINER,
        )

    override fun apply(
        operator: JexlOperator?,
        obj: Any?,
    ): List<PropertyResolver> =
        if (operator == JexlOperator.ARRAY_GET || operator == JexlOperator.ARRAY_SET || (operator == null && obj is Map<*, *>)) {
            mapResolvers
        } else {
            pojoResolves
        }
}
