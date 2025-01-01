package io.github.lengors.webscout.domain.jexl.services

import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.internal.introspection.DuckGetExecutor
import org.apache.commons.jexl3.internal.introspection.DuckSetExecutor
import org.apache.commons.jexl3.internal.introspection.Introspector
import org.apache.commons.jexl3.introspection.JexlPropertyGet
import org.apache.commons.jexl3.introspection.JexlPropertySet
import org.apache.commons.jexl3.introspection.JexlUberspect
import org.apache.commons.jexl3.introspection.JexlUberspect.PropertyResolver
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.cast

object DuckPropertyResolver : PropertyResolver {
    private val introspectorMethods: MutableMap<Class<*>, Optional<Method>> = ConcurrentHashMap()

    private fun getIntrospector(
        uberspect: JexlUberspect?,
        `object`: Any?,
    ): Introspector? =
        uberspect
            ?.takeUnless { `object` == null || `object` is JexlContext }
            ?.javaClass
            ?.let {
                introspectorMethods.computeIfAbsent(it) { key ->
                    Optional.ofNullable(ReflectionUtils.findMethod(key, "base"))
                }
            }?.getOrNull()
            ?.invoke(uberspect)
            ?.takeIf(Introspector::class::isInstance)
            ?.let(Introspector::class::cast)

    override fun getPropertyGet(
        uberspect: JexlUberspect?,
        `object`: Any?,
        identifier: Any?,
    ): JexlPropertyGet? =
        getIntrospector(uberspect, `object`)?.let { introspector ->
            DuckGetExecutor.discover(introspector, `object`?.javaClass, identifier) ?: Unit.let {
                val property =
                    when (identifier) {
                        is CharSequence, is Int -> identifier.toString()
                        else -> null
                    }
                property
                    ?.takeIf { it != identifier }
                    ?.let { DuckGetExecutor.discover(introspector, `object`?.javaClass, it) }
            }
        }

    override fun getPropertySet(
        uberspect: JexlUberspect?,
        `object`: Any?,
        identifier: Any?,
        argument: Any?,
    ): JexlPropertySet? =
        getIntrospector(uberspect, `object`)?.let { introspector ->
            DuckSetExecutor.discover(introspector, `object`?.javaClass, identifier, argument) ?: Unit.let {
                val property =
                    when (identifier) {
                        is CharSequence, is Int -> identifier.toString()
                        else -> null
                    }
                property
                    ?.takeIf { it != identifier }
                    ?.let { DuckSetExecutor.discover(introspector, `object`?.javaClass, it, argument) }
            }
        }
}
