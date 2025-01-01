package io.github.lengors.webscout.domain.jexl.models

data class JexlSimpleValueHolder<out T : Any>(
    override val context: JexlContextValueHolder,
    override val valueOrNull: T? = null,
) : JexlValueHolder<T>
